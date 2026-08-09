import {Component, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {Subscription, interval, startWith, switchMap, takeWhile} from 'rxjs';
import {TrainerRoadZwoExportClient} from 'infrastructure/client/trainer-road-zwo-export.client';

@Component({
  selector: 'tr-zwo-export',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatProgressBarModule],
  templateUrl: './tr-zwo-export.component.html',
  styleUrl: './tr-zwo-export.component.scss'
})
export class TrZwoExportComponent implements OnDestroy {
  scan: any = null;
  job: any = null;
  scanId = '';
  selectedCategories = new Set<string>();
  selectedWorkouts = new Set<string>();
  minDuration: number | null = null;
  maxDuration: number | null = null;
  minWorkoutLevel: number | null = null;
  maxWorkoutLevel: number | null = null;
  requestDelaySeconds = 10;
  busy = false;
  private subscription?: Subscription;

  constructor(private client: TrainerRoadZwoExportClient) {}

  ngOnDestroy() { this.subscription?.unsubscribe(); }

  startScan() {
    this.busy = true;
    this.scan = null;
    this.job = null;
    this.selectedCategories.clear();
    this.selectedWorkouts.clear();
    this.client.startScan(this.requestDelaySeconds).subscribe(value => {
      this.scanId = value.id;
      this.watchScan();
    });
  }

  private watchScan() {
    this.subscription?.unsubscribe();
    this.subscription = interval(1000).pipe(
      startWith(0),
      switchMap(() => this.client.getScan(this.scanId)),
      takeWhile(value => ['queued', 'running'].includes(value.status), true)
    ).subscribe(value => {
      this.scan = value;
      this.busy = ['queued', 'running'].includes(value.status);
      if (value.status === 'completed') this.selectedWorkouts.clear();
    });
  }

  toggleCategory(id: string, checked: boolean) {
    checked ? this.selectedCategories.add(id) : this.selectedCategories.delete(id);
    this.selectedWorkouts.clear();
  }

  matchingWorkouts() {
    const workouts = this.scan?.workouts ?? [];
    return workouts.filter((item: any) =>
      (this.selectedCategories.size === 0 || this.selectedCategories.has(item.categoryId)) &&
      (this.minDuration == null || item.durationMinutes >= this.minDuration) &&
      (this.maxDuration == null || item.durationMinutes <= this.maxDuration) &&
      (this.minWorkoutLevel == null || (item.workoutLevel != null && item.workoutLevel >= this.minWorkoutLevel)) &&
      (this.maxWorkoutLevel == null || (item.workoutLevel != null && item.workoutLevel <= this.maxWorkoutLevel))
    );
  }

  toggleWorkout(id: string, checked: boolean) { checked ? this.selectedWorkouts.add(id) : this.selectedWorkouts.delete(id); }

  scanErrorMessage(code: string | null | undefined) {
    const messages: Record<string, string> = {
      auth_expired: 'TrainerRoad login verlopen. Vernieuw de TrainerRoad-cookie in Config en probeer opnieuw.',
      rate_limited: 'TrainerRoad beperkt tijdelijk de aanvragen. Wacht even en probeer opnieuw.',
      trainerroad_response_invalid: 'TrainerRoad gaf een onverwacht antwoord. Bekijk het logbestand voor details.',
      network_or_internal_error: 'TrainerRoad kon niet worden bereikt of de scan liep intern fout.'
    };
    return messages[code ?? ''] ?? `TrainerRoad scan mislukt (${code ?? 'onbekende fout'}).`;
  }

  selectAllMatching() { this.matchingWorkouts().forEach(item => this.selectedWorkouts.add(item.id)); }

  startExport() {
    if (!this.scanId || this.selectedWorkouts.size === 0) return;
    this.busy = true;
    this.client.startJob({
      scanId: this.scanId,
      selectedWorkoutIds: Array.from(this.selectedWorkouts),
      categories: Array.from(this.selectedCategories),
      minDuration: this.minDuration,
      maxDuration: this.maxDuration,
      minWorkoutLevel: this.minWorkoutLevel,
      maxWorkoutLevel: this.maxWorkoutLevel,
      requestDelaySeconds: this.requestDelaySeconds
    }).subscribe(value => { this.job = value; this.watchJob(); });
  }

  private watchJob() {
    this.subscription?.unsubscribe();
    this.subscription = interval(1000).pipe(
      startWith(0),
      switchMap(() => this.client.getJob(this.job.id)),
      takeWhile(value => ['queued', 'running'].includes(value.status), true)
    ).subscribe(value => { this.job = value; this.busy = ['queued', 'running'].includes(value.status); });
  }

  cancelExport() { if (this.job?.id) this.client.cancelJob(this.job.id).subscribe(value => this.job = value); }

  download() { if (this.job?.downloadReady) window.open(this.client.downloadUrl(this.job.id), '_blank'); }
}
