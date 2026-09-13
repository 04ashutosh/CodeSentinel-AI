import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectUpload } from '../project-upload/project-upload';
import { AiChat } from '../ai-chat/ai-chat';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule,ProjectUpload,AiChat],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {}
