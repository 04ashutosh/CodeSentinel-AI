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

  ingestGit(repoUrl: string, branch: string, projectName: string): Observable<any> {
    return this.http.post('/api/v1/ingestion/git', {
      repoUrl,
      branch,
      projectName
    }, {
      headers: {
        'X-User-Email': 'test@codesentinel.com'
      }
    });
  }

  getProjectInventory(projectId: string): Observable<any> {
    return this.http.get(`/api/v1/parser/projects/${projectId}/inventory`);
  }

  getProjectGraph(projectId: string): Observable<any> {
    return this.http.get(`/api/v1/parser/projects/${projectId}/graph`);
  }

  askQuestion(projectId: string, question: string): Observable<any> {
    return this.http.post('/api/v1/ai/chat', {
      projectId,
      question
    });
  }
}