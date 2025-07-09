// Updated metric-card.component.ts with dark mode support and responsive fonts
import { Component, Input, EventEmitter, Output } from '@angular/core';
import { NgClass, NgIf } from '@angular/common';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-metric-card',
  standalone: true,
  imports: [NgClass, NgIf, FaIconComponent],
  template: `
    <div class="metric-card" role="region" [attr.aria-label]="label">
      <button
        class="info-icon"
        [attr.aria-label]="label + ' information'"
        (click)="toggleInfo()"
        (keydown.enter)="toggleInfo()"
        (keydown.space)="$event.preventDefault(); toggleInfo()"
      >
        <fa-icon [icon]="'info-circle'"></fa-icon>
      </button>

      <!-- Info dialog (hidden by default) - moved outside the card for consistent positioning -->
      <div
        *ngIf="showInfo"
        class="info-dialog metric-info-dialog"
        role="dialog"
        [attr.aria-labelledby]="'metric-info-title-' + sanitizeId(label)"
      >
        <div class="dialog-content">
          <h3 [id]="'metric-info-title-' + sanitizeId(label)">{{ label }} Information</h3>
          <p>{{ getInfoText() }}</p>
          <button
            class="close-dialog-btn"
            aria-label="Close information dialog"
            (click)="toggleInfo()"
            (keydown.enter)="toggleInfo()"
            (keydown.space)="$event.preventDefault(); toggleInfo()"
          >
            Close
          </button>
        </div>
      </div>

      <div class="metric-card-main-icon" aria-hidden="true">
        <fa-icon [icon]="icon"></fa-icon>
      </div>
      <div class="metric-label" [id]="'metric-label-' + sanitizeId(label)">{{ label }}</div>
      <div class="metric-value" aria-live="polite" [attr.aria-labelledby]="'metric-label-' + sanitizeId(label)">{{ value }}</div>

      <!-- Additional description for screen readers -->
      <div class="sr-only" aria-live="polite">
        {{ ariaDescription || label + ': ' + value }}
      </div>
    </div>

    <!-- Modal backdrop for dialog - added to create consistent behavior -->
    <div *ngIf="showInfo" class="modal-backdrop" (click)="closeInfoDialog()" role="presentation" aria-hidden="true"></div>
  `,
  styles: [
    `
      .metric-card {
        background-color: var(--card-bg);
        border-radius: 0.5rem;
        padding: 1.75rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        position: relative;
        height: 100%;
      }

      .info-icon {
        position: absolute;
        top: 1.5rem;
        right: 1.5rem;
        color: var(--neutral-400);
        font-size: 1rem;
        background: none;
        border: none;
        cursor: pointer;
        padding: 0;
        z-index: 2;
      }

      .metric-card-main-icon {
        background-color: var(--bg-color);
        width: 2.25rem;
        height: 2.25rem;
        border-radius: 0.25rem;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1rem;
      }

      .metric-card-main-icon fa-icon {
        color: var(--neutral-500);
        font-size: 1rem;
      }

      .metric-label {
        color: var(--text-color);
        font-family: var(--font-sans);
        font-size: 1.1875rem;
        font-style: normal;
        font-weight: 400;
        line-height: 1.2;
        margin-bottom: 0.75rem;
        padding-right: 2.5rem;
      }

      .metric-value {
        color: var(--text-color);
        font-family: var(--font-sans);
        font-size: 1.75rem;
        font-style: normal;
        font-weight: 700;
        line-height: 1.2;
      }

      .metric-info-dialog {
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background-color: var(--card-bg);
        color: var(--text-color);
        border-radius: 0.5rem;
        box-shadow: 0 0.25rem 0.75rem rgba(0, 0, 0, 0.2);
        z-index: 10;
        padding: 1.25rem;
        min-width: 17.5rem;
        max-width: 90%;
      }

      .dialog-content {
        display: flex;
        flex-direction: column;
      }

      .dialog-content h3 {
        margin-top: 0;
        margin-bottom: 0.625rem;
        font-size: 1.125rem;
        color: var(--text-color);
        font-family: var(--font-sans);
      }

      .dialog-content p {
        margin-bottom: 1.25rem;
        line-height: 1.5;
        color: var(--text-color);
        font-family: var(--font-sans);
      }

      .close-dialog-btn {
        align-self: flex-end;
        padding: 0.375rem 0.75rem;
        background-color: var(--accent-50);
        color: var(--button-text);
        border: none;
        border-radius: 0.25rem;
        cursor: pointer;
      }

      .modal-backdrop {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: rgba(0, 0, 0, 0.5);
        z-index: 5;
      }

      .sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }

      @media (max-width: 768px) {
        .metric-card {
          padding: 1.5rem 1rem;
        }

        .info-icon {
          top: 1.25rem;
          right: 1.25rem;
        }

        .metric-label {
          padding-right: 2.5rem;
          font-size: 1.05rem;
        }

        .metric-value {
          font-size: 1.5rem;
        }

        .metric-info-dialog {
          width: 85%;
          max-width: 85%;
        }
      }

      @media (max-width: 480px) {
        .metric-card {
          padding: 1.25rem 0.75rem;
        }

        .info-icon {
          top: 1rem;
          right: 1rem;
        }

        .metric-card-main-icon {
          width: 1.75rem;
          height: 1.75rem;
        }

        .metric-label {
          font-size: 1rem;
          margin-bottom: 0.5rem;
          padding-right: 2rem;
        }
      }
    `,
  ],
})
export class MetricCardComponent {
  @Input() icon: string = '';
  @Input() label: string = '';
  @Input() value: string | number = '';
  @Input() ariaDescription: string = ''; // Enhanced description for screen readers
  @Output() dialogStateChange = new EventEmitter<boolean>(); // Emit dialog state changes

  showInfo = false;
  private lastFocusedElement: HTMLElement | null = null;

  // Toggle info dialog
  toggleInfo(): void {
    this.lastFocusedElement = document.activeElement as HTMLElement;
    this.showInfo = !this.showInfo;
    this.dialogStateChange.emit(this.showInfo);

    if (this.showInfo) {
      // Focus the close button when dialog opens
      setTimeout(() => {
        const closeBtn = document.querySelector('.metric-info-dialog .close-dialog-btn') as HTMLElement;
        if (closeBtn) closeBtn.focus();
      }, 10);
    } else if (this.lastFocusedElement) {
      // Return focus when dialog closes
      this.lastFocusedElement.focus();
    }
  }

  // Close dialog when backdrop is clicked
  closeInfoDialog(): void {
    if (this.showInfo) {
      this.showInfo = false;
      this.dialogStateChange.emit(false);

      if (this.lastFocusedElement) {
        // Return focus when dialog closes
        setTimeout(() => {
          this.lastFocusedElement?.focus();
        }, 10);
      }
    }
  }

  // Generate descriptive text based on the metric type
  getInfoText(): string {
    if (this.label.includes('green journeys')) {
      return 'This metric shows the total number of eco-friendly journeys you have taken during the selected time period. Green journeys are trips that produce less CO₂ emissions compared to standard transportation options.';
    } else if (this.label.includes('trip rating')) {
      return 'This metric displays your average trip rating for the selected time period. Ratings are categorized as Excellent, Good, or Poor based on the environmental impact of your journeys.';
    } else if (this.label.includes('CO₂ emissions')) {
      return 'This metric shows your average carbon dioxide (CO₂) emissions in kilograms for the selected time period. Lower values indicate a smaller environmental impact from your journeys.';
    }

    return `Information about ${this.label}`;
  }

  // Create a safe ID from the label
  sanitizeId(text: string): string {
    return text.toLowerCase().replace(/[^a-z0-9]/g, '-');
  }
}
