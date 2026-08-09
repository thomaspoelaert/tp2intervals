import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';

@Injectable({providedIn: 'root'})
export class TrainerRoadZwoExportClient {
  constructor(private http: HttpClient) {}

  startScan(requestDelaySeconds = 10) {
    return this.http.post<any>('/api/trainer-road/zwo-export/scans', {requestDelaySeconds});
  }

  getScan(id: string) {
    return this.http.get<any>(`/api/trainer-road/zwo-export/scans/${id}`);
  }

  startJob(request: any) {
    return this.http.post<any>('/api/trainer-road/zwo-export/jobs', request);
  }

  getJob(id: string) {
    return this.http.get<any>(`/api/trainer-road/zwo-export/jobs/${id}`);
  }

  cancelJob(id: string) {
    return this.http.post<any>(`/api/trainer-road/zwo-export/jobs/${id}/cancel`, {});
  }

  downloadUrl(id: string) {
    return `/api/trainer-road/zwo-export/jobs/${id}/download`;
  }
}
