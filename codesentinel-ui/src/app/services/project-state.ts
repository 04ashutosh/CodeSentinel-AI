import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ProjectState {
  private selectedProjectIdSource = new BehaviorSubject<string | null>(null);
  selectedProjectId$ = this.selectedProjectIdSource.asObservable();

  setProjectId(id: string) {
    this.selectedProjectIdSource.next(id);
  }

  getProjectId(): string | null {
    return this.selectedProjectIdSource.getValue();
  }
}