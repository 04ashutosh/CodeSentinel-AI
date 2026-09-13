import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../../services/api';
import { ProjectState } from '../../services/project-state';

@Component({
  selector: 'app-project-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './project-upload.html',
  styleUrl: './project-upload.scss'
})
export class ProjectUpload {
  selectedFile: File | null = null;
  uploading = false;
  message = '';

  constructor(private api: Api, private projectState: ProjectState) {}

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
}