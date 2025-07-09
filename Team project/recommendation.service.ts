import { Injectable } from '@angular/core';

/**
 * Service to handle transport recommendations based on emissions data
 */
@Injectable({
  providedIn: 'root',
})
export class RecommendationService {
  // Transport modes sorted from most to least green
  private transportRanking: string[] = ['WALKING', 'CYCLING', 'TRAIN', 'ELECTRIC_CAR', 'BUS', 'CAR'];

  // CO2 emissions in kg per km for each transport mode
  private transportEmissions: { [key: string]: number } = {
    WALKING: 0,
    CYCLING: 0,
    TRAIN: 0.041,
    ELECTRIC_CAR: 0.053,
    BUS: 0.105,
    CAR: 0.192,
  };

  // Display names for the transport modes
  private transportDisplayNames: { [key: string]: string } = {
    WALKING: 'walking',
    CYCLING: 'cycling',
    TRAIN: 'train',
    ELECTRIC_CAR: 'electric car',
    BUS: 'bus',
    CAR: 'car',
  };

  // Icons for the transport modes
  private transportIcons: { [key: string]: string } = {
    WALKING: 'fa-walking',
    CYCLING: 'fa-bicycle',
    TRAIN: 'fa-train',
    ELECTRIC_CAR: 'fa-charging-station',
    BUS: 'fa-bus',
    CAR: 'fa-car',
  };

  constructor() {}

  /**
   * Get recommendations for greener transport modes than the current one
   * @param currentMode The current transport mode
   * @param distance The trip distance in km
   * @returns Array of recommendations with transport mode and CO2 reduction %
   */
  getRecommendations(currentMode: string, distance: number): { mode: string; displayName: string; icon: string; reduction: number }[] {
    // Standardize the current mode to match our keys
    const normalizedMode = this.normalizeTransportMode(currentMode);

    // Find the current mode's index in our ranking
    const currentIndex = this.transportRanking.indexOf(normalizedMode);

    // If mode not found or already the greenest, return empty array
    if (currentIndex === -1 || currentIndex === 0) {
      return [];
    }

    // Get the emission value for the current mode
    const currentEmission = this.transportEmissions[normalizedMode] * distance;

    // Get all greener transport modes
    const recommendations = [];
    for (let i = 0; i < currentIndex; i++) {
      const greenerMode = this.transportRanking[i];
      const greenerEmission = this.transportEmissions[greenerMode] * distance;

      // Calculate reduction percentage
      const reductionPercentage = Math.round(((currentEmission - greenerEmission) / currentEmission) * 100);

      recommendations.push({
        mode: greenerMode,
        displayName: this.transportDisplayNames[greenerMode],
        icon: this.transportIcons[greenerMode],
        reduction: reductionPercentage,
      });
    }

    return recommendations;
  }

  /**
   * Normalize transport mode string to match our internal format
   * @param transportMode Original transport mode string
   * @returns Normalized transport mode
   */
  private normalizeTransportMode(transportMode: string): string {
    if (!transportMode) return 'CAR'; // Default to car if undefined

    const mode = transportMode.toUpperCase();

    if (mode.includes('WALK')) return 'WALKING';
    if (mode.includes('CYCLE') || mode.includes('BIKE')) return 'CYCLING';
    if (mode.includes('TRAIN')) return 'TRAIN';
    if (mode.includes('ELECTRIC')) return 'ELECTRIC_CAR';
    if (mode.includes('BUS')) return 'BUS';
    if (mode.includes('CAR') && !mode.includes('ELECTRIC')) return 'CAR';

    // Default to car if no match
    return 'CAR';
  }

  /**
   * Get the next recommendation in the cycle
   * @param recommendations The array of recommendations
   * @param currentIndex The current recommendation index
   * @returns The next recommendation index
   */
  getNextRecommendation(recommendations: any[], currentIndex: number): number {
    if (recommendations.length === 0) return -1;

    return (currentIndex + 1) % recommendations.length;
  }
}
