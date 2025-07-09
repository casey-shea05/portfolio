import { Component, OnInit, AfterViewInit, HostListener, ElementRef } from '@angular/core';
import { NgClass, NgIf, NgStyle, NgFor } from '@angular/common';
import { Chart } from 'chart.js/auto';
import { registerables } from 'chart.js';
import { HttpClient } from '@angular/common/http';
import { RecommendationService } from './recommendation.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { MetricCardComponent } from './metric-card.component';

// Register all Chart.js components
Chart.register(...registerables);

@Component({
  selector: 'jhi-insights-dashboard',
  standalone: true,
  imports: [NgClass, NgIf, NgStyle, NgFor, FaIconComponent, MetricCardComponent],
  templateUrl: './insights-dashboard.component.html',
  // Add this 'styles' array in addition to your styleUrl
  styles: [
    `
      :host {
        --dashboard-bg-color: var(--neutral-100);
        --dashboard-card-bg: white;
        --dashboard-text-dark: var(--neutral-700);
        --dashboard-text-medium: var(--neutral-500);
        --dashboard-icon-bg: var(--neutral-100);
        --accent: var(--accent-200);
      }

      :host-context(.dark-mode) {
        --dashboard-bg-color: var(--dark-900);
        --dashboard-card-bg: var(--dark-700);
        --dashboard-text-dark: white;
        --dashboard-text-medium: white;
        --dashboard-icon-bg: var(--dark-900);
        --accent: #fe6510;
      }

      .filter-bar-container {
        display: flex;
        align-items: center;
        background-color: var(--dashboard-card-bg);
        border-bottom: 1px solid var(--dashboard-card-bg);
        padding-top: 36px;
        padding-left: 40px;
      }

      .dashboard-content {
        background-color: var(--dashboard-bg-color) !important;
      }

      .chart-card,
      .comparison-card,
      .trip-details-card,
      .coal-comparison-card,
      .recommendation-card,
      ::ng-deep app-metric-card .metric-card {
        background-color: var(--dashboard-card-bg) !important;
      }

      .chart-filter-tabs {
        background-color: var(--dashboard-card-bg) !important;
      }

      .filter-tab {
        color: var(--dashboard-text-dark) !important;

        &.active {
          color: var(--accent) !important;

          &:after {
            background-color: var(--accent) !important;
          }
        }

        &:hover:not(.active) {
          color: var(--accent) !important;
        }
      }

      .icon-box,
      ::ng-deep app-metric-card .metric-card-main-icon {
        background-color: var(--dashboard-icon-bg) !important;
      }

      .trip-details-title,
      .trip-details,
      .trip-route,
      .trip-text,
      .coal-title,
      .coal-value,
      .coal-label,
      .recommendation-title,
      .recommendation-text,
      .reduction-value,
      .emission-value,
      .emission-label,
      .emission-description,
      ::ng-deep app-metric-card .metric-value {
        color: var(--dashboard-text-dark) !important;
      }

      .legend-item,
      .week-label,
      ::ng-deep app-metric-card .metric-label {
        color: var(--dashboard-text-dark) !important;
      }

      .transport-link {
        color: var(--accent) !important;
      }
    `,
  ],
  styleUrl: './insights-dashboard.component.scss',
})
export class InsightsDashboardComponent implements OnInit, AfterViewInit {
  selectedPeriod = 'week';
  activeTab = 'emissions';

  // Dashboard metrics
  totalJourneys = 0;
  greenRating = '';
  co2Emissions = 0;

  // Chart data
  chartLabels: string[] = [];
  yourEmissionsData: number[] = [];
  avgEmissionsData: number[] = [];

  // Comparison data
  comparisonPercentage = 0; // New property for comparison percentage
  isBetterThanAverage = true; // To determine if user emits less or more than average

  // Rating chart data
  ratingLabels: string[] = [];
  ratingData: number[] = [];
  ratingColors: string[] = ['#e27588', '#ffc42c', '#e44f3f']; // Excellent, Good, Poor - pure gray tones

  // Chart instances
  private ratingPieChart?: Chart;
  private emissionsLineChart?: Chart;

  // Highest emission trip data
  highestEmissionTrip: any = {
    found: false,
    date: '',
    startPoint: '',
    endPoint: '',
    distance: 0,
    transportMode: '',
    co2Value: 0,
    coalEquivalent: 0,
  };

  // Recommendation data
  recommendations: { mode: string; displayName: string; icon: string; reduction: number }[] = [];
  currentRecommendationIndex = 0;

  // Flag to track if data is loading
  isLoading = true;

  // Dialog states
  showChartInfo = false;
  showTripInfoDialog = false;
  showRecommendationInfoDialog = false;
  activeMetricDialog: string | null = null;

  // Last focused element (for returning focus after dialog close)
  private lastFocusedElement: HTMLElement | null = null;

  constructor(
    private http: HttpClient,
    private recommendationService: RecommendationService,
    private elementRef: ElementRef,
  ) {}

  ngOnInit(): void {
    // Check for dark mode on initialization
    const savedMode = localStorage.getItem('darkMode');
    const isDarkMode = savedMode ? JSON.parse(savedMode) : false;
    document.documentElement.classList.toggle('dark-mode', isDarkMode);

    this.loadData();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initCharts();
    }, 100);
  }

  // Handle escape key to close dialogs
  @HostListener('document:keydown.escape', ['$event'])
  handleEscapeKey(event: KeyboardEvent): void {
    if (this.showChartInfo || this.showTripInfoDialog || this.showRecommendationInfoDialog || this.activeMetricDialog !== null) {
      this.closeAllDialogs();
      event.preventDefault();
    }
  }

  // Listen for dark mode changes
  @HostListener('window:storage', ['$event'])
  onStorageChange(event: StorageEvent): void {
    // Check if darkMode setting was changed
    if (event.key === 'darkMode') {
      // Re-initialize charts to update their styles
      setTimeout(() => {
        this.initCharts();
      }, 100);
    }
  }

  // Handle tab key trap for dialogs
  @HostListener('document:keydown.tab', ['$event'])
  handleTabKey(event: KeyboardEvent): void {
    if (!this.showChartInfo && !this.showTripInfoDialog && !this.showRecommendationInfoDialog) {
      return; // No dialog is open, normal tab behavior
    }

    const dialog = this.getOpenDialog();
    if (!dialog) return;

    const focusableElements = this.getFocusableElements(dialog);
    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    // If shift + tab and first element is focused, move to last element
    if (event.shiftKey && document.activeElement === firstElement) {
      lastElement.focus();
      event.preventDefault();
    }
    // If tab and last element is focused, move to first element
    else if (!event.shiftKey && document.activeElement === lastElement) {
      firstElement.focus();
      event.preventDefault();
    }
  }

  // Helper to get the currently open dialog
  private getOpenDialog(): HTMLElement | null {
    if (this.showChartInfo) {
      return this.elementRef.nativeElement.querySelector('.chart-info-dialog');
    } else if (this.showTripInfoDialog) {
      return this.elementRef.nativeElement.querySelector('.trip-details-card .info-dialog');
    } else if (this.showRecommendationInfoDialog) {
      return this.elementRef.nativeElement.querySelector('.recommendation-card .info-dialog');
    }
    return null;
  }

  // Get focusable elements within a container
  private getFocusableElements(container: HTMLElement): HTMLElement[] {
    const selector = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';
    return Array.from(container.querySelectorAll(selector)) as HTMLElement[];
  }

  setPeriod(period: string): void {
    this.selectedPeriod = period;
    this.isLoading = true;
    this.loadData();
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    setTimeout(() => {
      this.initCharts();
    }, 100);
  }

  // Toggle chart info dialog - UPDATED
  toggleChartInfo(): void {
    this.lastFocusedElement = document.activeElement as HTMLElement;
    this.showChartInfo = !this.showChartInfo;
    this.showTripInfoDialog = false;
    this.showRecommendationInfoDialog = false;
    this.activeMetricDialog = null;

    if (this.showChartInfo) {
      // Set focus to dialog close button after it opens
      setTimeout(() => {
        const closeBtn = this.elementRef.nativeElement.querySelector('.chart-info-dialog .close-dialog-btn');
        if (closeBtn) closeBtn.focus();
      }, 10);
    } else if (this.lastFocusedElement) {
      // Return focus when dialog closes
      setTimeout(() => {
        this.lastFocusedElement?.focus();
      }, 10);
    }
  }

  // Toggle trip info dialog - UPDATED
  toggleTripInfoDialog(): void {
    this.lastFocusedElement = document.activeElement as HTMLElement;
    this.showTripInfoDialog = !this.showTripInfoDialog;
    this.showChartInfo = false;
    this.showRecommendationInfoDialog = false;
    this.activeMetricDialog = null;

    if (this.showTripInfoDialog) {
      setTimeout(() => {
        const closeBtn = this.elementRef.nativeElement.querySelector('.trip-details-card .info-dialog .close-dialog-btn');
        if (closeBtn) closeBtn.focus();
      }, 10);
    } else if (this.lastFocusedElement) {
      setTimeout(() => {
        this.lastFocusedElement?.focus();
      }, 10);
    }
  }

  // Toggle recommendation info dialog - UPDATED
  toggleRecommendationInfoDialog(): void {
    this.lastFocusedElement = document.activeElement as HTMLElement;
    this.showRecommendationInfoDialog = !this.showRecommendationInfoDialog;
    this.showChartInfo = false;
    this.showTripInfoDialog = false;
    this.activeMetricDialog = null;

    if (this.showRecommendationInfoDialog) {
      setTimeout(() => {
        const closeBtn = this.elementRef.nativeElement.querySelector('.recommendation-card .info-dialog .close-dialog-btn');
        if (closeBtn) closeBtn.focus();
      }, 10);
    } else if (this.lastFocusedElement) {
      setTimeout(() => {
        this.lastFocusedElement?.focus();
      }, 10);
    }
  }

  // Close all dialogs - UPDATED
  closeAllDialogs(): void {
    const wasDialogOpen =
      this.showChartInfo || this.showTripInfoDialog || this.showRecommendationInfoDialog || this.activeMetricDialog !== null;

    this.showChartInfo = false;
    this.showTripInfoDialog = false;
    this.showRecommendationInfoDialog = false;
    this.activeMetricDialog = null;

    // Need to manually trigger close on metric cards if they're open
    const metricCards = this.elementRef.nativeElement.querySelectorAll('app-metric-card');
    metricCards.forEach((card: any) => {
      if (card.componentInstance && card.componentInstance.showInfo) {
        card.componentInstance.closeInfoDialog();
      }
    });

    if (wasDialogOpen && this.lastFocusedElement) {
      setTimeout(() => {
        this.lastFocusedElement?.focus();
      }, 10);
    }
  }

  // Handle metric card dialog state changes
  onMetricDialogStateChange(isOpen: boolean, metricLabel: string): void {
    if (isOpen) {
      this.activeMetricDialog = metricLabel;
      // Close any other dialogs that might be open
      this.showChartInfo = false;
      this.showTripInfoDialog = false;
      this.showRecommendationInfoDialog = false;
    } else if (this.activeMetricDialog === metricLabel) {
      this.activeMetricDialog = null;
    }
  }

  // Calculate and return rating percentages for screen readers
  getRatingPercentage(index: number): number {
    if (this.ratingLabels.length === 1 && this.ratingLabels[0] === 'No Data') {
      return 0;
    }

    const total = this.ratingData.reduce((sum, val) => sum + val, 0);
    if (total === 0) return 0;

    return Math.round((this.ratingData[index] / total) * 100);
  }

  // Enhanced chart summary for screen readers
  getChartSummary(): string {
    if (this.yourEmissionsData.length === 0 || this.avgEmissionsData.length === 0) {
      return 'No emissions data available for this period.';
    }

    const yourTotal = this.yourEmissionsData.reduce((sum, val) => sum + val, 0);
    const avgTotal = this.avgEmissionsData.reduce((sum, val) => sum + val, 0);

    const comparisonText = this.isBetterThanAverage
      ? `You emit ${this.comparisonPercentage}% less CO₂ than the average user.`
      : `You emit ${this.comparisonPercentage}% more CO₂ than the average user.`;

    // Find highest emission day/week/month
    let highestIndex = 0;
    let highestValue = 0;

    this.yourEmissionsData.forEach((value, index) => {
      if (value > highestValue) {
        highestValue = value;
        highestIndex = index;
      }
    });

    let timePeriodType;
    switch (this.selectedPeriod) {
      case 'week':
        timePeriodType = 'day';
        break;
      case 'month':
        timePeriodType = 'week';
        break;
      case 'year':
        timePeriodType = 'month';
        break;
      default:
        timePeriodType = 'period';
    }

    let trendAnalysis = '';
    if (this.yourEmissionsData.length > 1) {
      const firstValue = this.yourEmissionsData[0];
      const lastValue = this.yourEmissionsData[this.yourEmissionsData.length - 1];

      if (lastValue < firstValue) {
        trendAnalysis = 'Your emissions show a decreasing trend over the period.';
      } else if (lastValue > firstValue) {
        trendAnalysis = 'Your emissions show an increasing trend over the period.';
      } else {
        trendAnalysis = 'Your emissions remain stable over the period.';
      }
    }

    return `Your total emissions are ${yourTotal.toFixed(1)} kg CO₂, while the average user emits ${avgTotal.toFixed(1)} kg CO₂. ${comparisonText} Your highest emission ${timePeriodType} was ${this.chartLabels[highestIndex]} with ${highestValue.toFixed(1)} kg CO₂. ${trendAnalysis}`;
  }

  // Enhanced rating chart summary for screen readers
  getRatingChartSummary(): string {
    if (this.ratingLabels.length === 1 && this.ratingLabels[0] === 'No Data') {
      return 'No rating data available for this period.';
    }

    const total = this.ratingData.reduce((sum, val) => sum + val, 0);

    if (total === 0) {
      return 'No trips recorded in this period.';
    }

    // Create detailed breakdown
    const ratingSummary = this.ratingLabels
      .map((label, index) => {
        const count = this.ratingData[index];
        const percentage = Math.round((count / total) * 100);
        return `${count} trips (${percentage}%) rated as ${label}`;
      })
      .join(', ');

    // Determine dominant rating
    let maxIndex = 0;
    let maxValue = 0;

    this.ratingData.forEach((value, index) => {
      if (value > maxValue) {
        maxValue = value;
        maxIndex = index;
      }
    });

    const dominantRating = this.ratingLabels[maxIndex];

    return `Out of ${total} total trips, ${ratingSummary}. The most common rating is ${dominantRating}.`;
  }

  // Get detailed recommendation information for screen readers
  getRecommendationDetails(): string {
    if (!this.getCurrentRecommendation() || !this.highestEmissionTrip.found) {
      return '';
    }

    const currentRec = this.getCurrentRecommendation();
    const origCO2 = this.highestEmissionTrip.co2Value;
    const newCO2 = origCO2 * (1 - currentRec!.reduction / 100);

    return `By switching to ${currentRec!.displayName}, your CO₂ emissions would decrease from
           ${origCO2.toFixed(1)} kg to approximately ${newCO2.toFixed(1)} kg for this journey.
           There are ${this.recommendations.length} alternative transportation options available.
           You are currently viewing option ${this.currentRecommendationIndex + 1} of ${this.recommendations.length}.`;
  }

  loadData(): void {
    // Call API to get green journeys count
    this.http.get<number>(`/api/insights-dashboard/green-journeys-count/${this.selectedPeriod}`).subscribe({
      next: (count: number) => {
        this.totalJourneys = count;
      },
      error: (error: any) => {
        console.error('Error loading green journeys count', error);
        this.totalJourneys = 0;
      },
    });

    // Call API to get average rating
    this.http
      .get(`/api/insights-dashboard/average-rating/${this.selectedPeriod}`, {
        responseType: 'text',
      })
      .subscribe({
        next: rating => {
          this.greenRating = rating;
        },
        error: (error: any) => {
          console.error('Error loading average rating', error);
          this.greenRating = 'No Data';
        },
      });

    // Call API to get CO2 emissions
    this.http.get<number>(`/api/insights-dashboard/co2-emissions/${this.selectedPeriod}`).subscribe({
      next: (emissions: number) => {
        this.co2Emissions = emissions;
      },
      error: (error: any) => {
        console.error('Error loading CO2 emissions', error);
        this.co2Emissions = 0;
      },
    });

    // Call API to get emissions comparison data
    this.http.get<any>(`/api/insights-dashboard/emissions-comparison/${this.selectedPeriod}`).subscribe({
      next: (comparisonData: any) => {
        this.comparisonPercentage = comparisonData.percentageDiff || 0;
        this.isBetterThanAverage = comparisonData.isBetterThanAverage !== undefined ? comparisonData.isBetterThanAverage : true;
      },
      error: (error: any) => {
        console.error('Error loading emissions comparison data', error);
        this.comparisonPercentage = 0;
        this.isBetterThanAverage = true;
      },
    });

    // Call API to get emissions chart data
    this.http.get<any>(`/api/insights-dashboard/emissions-chart-data/${this.selectedPeriod}`).subscribe({
      next: (chartData: any) => {
        this.chartLabels = chartData.labels || [];
        this.yourEmissionsData = chartData.yourData || [];
        this.avgEmissionsData = chartData.avgData || [];
        this.isLoading = false;
        this.updateCharts();

        // If the comparison API call failed, use the chart data as fallback
        if (this.comparisonPercentage === 0) {
          this.calculateComparisonPercentage();
        }
      },
      error: (error: any) => {
        console.error('Error loading emissions chart data', error);
        this.isLoading = false;

        // Fallback to dummy data in case of error
        this.setFallbackChartData();
        this.updateCharts();

        // Only reset these if not set by comparison endpoint
        if (this.comparisonPercentage === 0) {
          this.comparisonPercentage = 0;
          this.isBetterThanAverage = true;
        }
      },
    });

    // Call API to get rating chart data
    this.http.get<any>(`/api/insights-dashboard/rating-chart-data/${this.selectedPeriod}`).subscribe({
      next: (chartData: any) => {
        // Prepare data for pie chart
        this.ratingLabels = ['Excellent', 'Good', 'Poor'];
        this.ratingData = [chartData.excellent || 0, chartData.good || 0, chartData.poor || 0];

        // If there's no data, show placeholder message
        if (chartData.total === 0) {
          this.ratingData = [1]; // Show a single slice
          this.ratingLabels = ['No Data'];
          this.ratingColors = ['#e0e0e0'];
        } else {
          this.ratingColors = ['#e27588', '#ffc42c', '#e44f3f']; // Excellent, Good, Poor - pure gray tones
        }

        // Update the chart
        if (this.activeTab === 'rating') {
          this.initRatingPieChart();
        }
      },
      error: (error: any) => {
        console.error('Error loading rating chart data', error);

        // Fallback for rating data in case of error
        this.ratingLabels = ['No Data'];
        this.ratingData = [1];
        this.ratingColors = ['#e0e0e0'];

        if (this.activeTab === 'rating') {
          this.initRatingPieChart();
        }
      },
    });

    // Call API to get highest emission trip
    this.http.get<any>(`/api/insights-dashboard/highest-emission-trip/${this.selectedPeriod}`).subscribe({
      next: (tripData: any) => {
        this.highestEmissionTrip = tripData;

        // Generate recommendations based on the highest emission trip
        if (this.highestEmissionTrip.found) {
          this.generateRecommendations();
        } else {
          this.recommendations = [];
        }
      },
      error: (error: any) => {
        console.error('Error loading highest emission trip data', error);
        this.highestEmissionTrip = { found: false };
        this.recommendations = [];
      },
    });
  }

  // Calculate the percentage difference between user emissions and average emissions
  // This is a fallback method in case the dedicated comparison endpoint fails
  calculateComparisonPercentage(): void {
    // Sum up all emissions for both user and average
    const totalUserEmissions = this.yourEmissionsData.reduce((sum, val) => sum + val, 0);
    const totalAvgEmissions = this.avgEmissionsData.reduce((sum, val) => sum + val, 0);

    // Check if we have valid data
    if (totalAvgEmissions > 0 && totalUserEmissions > 0) {
      if (totalUserEmissions < totalAvgEmissions) {
        // User emits less than average (good)
        this.comparisonPercentage = Math.round(((totalAvgEmissions - totalUserEmissions) / totalAvgEmissions) * 100);
        this.isBetterThanAverage = true;
      } else {
        // User emits more than average (not so good)
        this.comparisonPercentage = Math.round(((totalUserEmissions - totalAvgEmissions) / totalAvgEmissions) * 100);
        this.isBetterThanAverage = false;
      }
    } else {
      // No valid data, set default values
      this.comparisonPercentage = 0;
      this.isBetterThanAverage = true;
    }

    console.log(
      `Fallback comparison calculation: ${this.comparisonPercentage}% ${this.isBetterThanAverage ? 'better' : 'worse'} than average`,
    );
  }

  private setFallbackChartData(): void {
    switch (this.selectedPeriod) {
      case 'week':
        this.chartLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        this.yourEmissionsData = [0, 0, 0, 0, 0, 0, 0];
        this.avgEmissionsData = [0, 0, 0, 0, 0, 0, 0];
        break;
      case 'month':
        this.chartLabels = ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
        this.yourEmissionsData = [0, 0, 0, 0];
        this.avgEmissionsData = [0, 0, 0, 0];
        break;
      case 'year':
        this.chartLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        this.yourEmissionsData = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
        this.avgEmissionsData = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
        break;
    }
  }

  // Method to detect dark mode
  private isDarkMode(): boolean {
    return document.documentElement.classList.contains('dark-mode');
  }

  private initCharts(): void {
    if (this.activeTab === 'rating') {
      this.initRatingPieChart();
    } else if (this.activeTab === 'emissions') {
      this.initEmissionsLineChart();
    }
  }

  private initRatingPieChart(): void {
    if (this.ratingPieChart) {
      this.ratingPieChart.destroy();
    }

    const pieCanvas = document.getElementById('ratingPieChart') as HTMLCanvasElement | null;
    if (pieCanvas === null) {
      console.error('Pie chart canvas element not found');
      return;
    }

    const ctx = pieCanvas.getContext('2d');
    if (!ctx) {
      console.error('Could not get 2D context for pie chart');
      return;
    }

    // Check if dark mode is active
    const darkMode = this.isDarkMode();

    // Enhanced accessible chart options
    this.ratingPieChart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: this.ratingLabels,
        datasets: [
          {
            data: this.ratingData,
            backgroundColor: this.ratingColors,
            borderColor: Array(this.ratingColors.length).fill(darkMode ? '#333333' : '#ffffff'),
            borderWidth: 1,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false,
            position: 'right',
            labels: {
              color: darkMode ? '#f0f0f0' : '#333333',
            },
          },
          tooltip: {
            backgroundColor: darkMode ? 'rgba(50, 50, 50, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: darkMode ? '#f0f0f0' : '#333333',
            bodyColor: darkMode ? '#cccccc' : '#666666',
            borderColor: darkMode ? '#555555' : '#dddddd',
            borderWidth: 1,
            padding: 10,
            callbacks: {
              label: function (context) {
                const label = context.label || '';
                const value = context.raw as number;
                const total = context.dataset.data.reduce((acc: number, val: number) => acc + val, 0);
                const percentage = Math.round((value / total) * 100);
                return `${label}: ${value} (${percentage}%)`;
              },
            },
          },
        },
      },
    });
  }

  private initEmissionsLineChart(): void {
    if (this.emissionsLineChart) {
      this.emissionsLineChart.destroy();
    }

    const lineCanvas = document.getElementById('emissionsLineChart') as HTMLCanvasElement | null;
    if (lineCanvas == null) {
      console.error('Line chart canvas element not found');
      return;
    }

    const ctx = lineCanvas.getContext('2d');
    if (!ctx) {
      console.error('Could not get 2D context for line chart');
      return;
    }

    // Check if dark mode is active
    const darkMode = this.isDarkMode();

    // Get grid color based on dark mode
    const gridColor = darkMode ? '#333333' : '#f0f0f0';
    const textColor = darkMode ? '#f0f0f0' : '#333333';

    // Enhanced accessible chart options
    this.emissionsLineChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.chartLabels,
        datasets: [
          {
            label: 'You',
            data: this.yourEmissionsData,
            borderColor: '#ebb000',
            backgroundColor: 'transparent',
            borderWidth: 2,
            tension: 0.4,
            pointRadius: 2,
          },
          {
            label: 'Average user',
            data: this.avgEmissionsData,
            borderColor: '#de356a',
            backgroundColor: 'transparent',
            borderWidth: 2,
            tension: 0.4,
            pointRadius: 2,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: {
            beginAtZero: true,
            title: {
              display: true,
              text: 'CO₂ Emissions (kg)',
              color: textColor,
            },
            ticks: {
              color: textColor,
            },
            grid: {
              color: gridColor,
            },
          },
          x: {
            grid: {
              display: false,
            },
            ticks: {
              color: textColor,
            },
            title: {
              display: true,
              text: this.selectedPeriod === 'week' ? 'Days' : this.selectedPeriod === 'month' ? 'Weeks' : 'Months',
              color: textColor,
            },
          },
        },
        plugins: {
          legend: {
            display: false, // Hide default legend since we have custom one
          },
          tooltip: {
            backgroundColor: darkMode ? 'rgba(50, 50, 50, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: darkMode ? '#f0f0f0' : '#333333',
            bodyColor: darkMode ? '#cccccc' : '#666666',
            borderColor: darkMode ? '#555555' : '#dddddd',
            borderWidth: 1,
            padding: 10,
          },
        },
      },
    });
  }

  private updateCharts(): void {
    if (this.activeTab === 'rating') {
      this.initRatingPieChart();
    } else if (this.activeTab === 'emissions') {
      this.initEmissionsLineChart();
    }
  }

  /**
   * Generate recommendations based on the highest emission trip
   */
  generateRecommendations(): void {
    if (!this.highestEmissionTrip.found) {
      this.recommendations = [];
      return;
    }

    // Get recommendations from the service
    this.recommendations = this.recommendationService.getRecommendations(
      this.highestEmissionTrip.transportMode,
      this.highestEmissionTrip.distance,
    );

    // Reset the recommendation index
    this.currentRecommendationIndex = this.recommendations.length > 0 ? 0 : -1;
  }

  /**
   * Get the current recommendation to display
   */
  getCurrentRecommendation(): { mode: string; displayName: string; icon: string; reduction: number } | null {
    if (this.recommendations.length === 0 || this.currentRecommendationIndex < 0) {
      return null;
    }
    return this.recommendations[this.currentRecommendationIndex];
  }

  /**
   * Cycle to the next recommendation
   */
  cycleRecommendation(): void {
    if (this.recommendations.length === 0) return;

    this.currentRecommendationIndex = this.recommendationService.getNextRecommendation(
      this.recommendations,
      this.currentRecommendationIndex,
    );
  }

  getFormattedPeriod(): string {
    switch (this.selectedPeriod) {
      case 'week':
        return 'This week';
      case 'month':
        return 'This month';
      case 'year':
        return 'This year';
      default:
        return 'This ' + this.selectedPeriod;
    }
  }
}
