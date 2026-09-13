import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../../services/api';
import { ProjectState } from '../../services/project-state';

import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-project-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-upload.html',
  styleUrl: './project-upload.scss'
})
export class ProjectUpload {
  mode: 'zip' | 'git' = 'zip';
  selectedFile: File | null = null;
  
  gitUrl = '';
  gitBranch = 'main';
  gitProjectName = '';
  
  uploading = false;
  message = '';

  constructor(private api: Api, private projectState: ProjectState) {}

  setMode(mode: 'zip' | 'git') {
    this.mode = mode;
    this.message = '';
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0] as File;
  }

  upload() {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.message = 'Uploading project...';

    // Using the file name without extension as the project name
    const projectName = this.selectedFile.name.replace('.zip', '');

    this.api.uploadProject(this.selectedFile, projectName).subscribe({
      next: (response) => {
        this.uploading = false;
        this.message = 'Project uploaded successfully!';
        
        // Save the project ID to state so the chat component can use it
        if (response.data && response.data.id) {
          this.projectState.setProjectId(response.data.id.toString());
        }
      },
      error: (err) => {
        this.uploading = false;
        this.message = 'Upload failed: ' + err.message;
      }
    });
  }

  uploadGit() {
    if (!this.gitUrl || !this.gitProjectName) return;

    this.uploading = true;
    this.message = 'Cloning and ingesting project... (This may take a minute)';

    this.api.ingestGit(this.gitUrl, this.gitBranch, this.gitProjectName).subscribe({
      next: (response) => {
        this.uploading = false;
        this.message = 'Git project ingested successfully!';
        
        if (response.data && response.data.id) {
          this.projectState.setProjectId(response.data.id.toString());
        }
      },
      error: (err) => {
        this.uploading = false;
        this.message = 'Ingestion failed: ' + err.message;
      }
    });
  }
}