import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectUpload } from '../project-upload/project-upload';
import { FileInventory } from '../file-inventory/file-inventory';
import { AiChat } from '../ai-chat/ai-chat';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ProjectUpload, FileInventory, AiChat],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  activeTab: 'upload' | 'inventory' = 'upload';

  setTab(tab: 'upload' | 'inventory') {
    this.activeTab = tab;
  }
}