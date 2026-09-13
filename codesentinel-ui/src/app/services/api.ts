import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class Api {
  constructor(private http: HttpClient) {}

  uploadProject(file: File, name: string): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('name', name);
    
    return this.http.post('/api/v1/ingestion/upload', formData, {
      headers: {
        'X-User-Email': 'test@codesentinel.com'
      }
    });
  }

  askQuestion(projectId: string, question: string): Observable<any> {
    return this.http.post('/api/v1/ai/chat', {
      projectId,
      question
    });
  }
}