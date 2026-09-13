import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';
import { ProjectState } from '../../services/project-state';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat.html',
  styleUrl: './ai-chat.scss'
})
export class AiChat implements OnInit, OnDestroy {
  projectId: string | null = null;
  question = '';
  chatHistory: { role: 'user' | 'agent', text: string }[] = [];
  loading = false;
  private sub?: Subscription;

  constructor(private api: Api, private projectState: ProjectState) {}

  ngOnInit() {
    this.sub = this.projectState.selectedProjectId$.subscribe(id => {
      this.projectId = id;
      if (id) {
        this.chatHistory.push({
          role: 'agent',
          text: `Project ID ${id} is ready. What would you like to know about it?`
        });
      }
    });
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
  }

  ask() {
    if (!this.question.trim() || !this.projectId) return;

    const userQ = this.question;
    this.chatHistory.push({ role: 'user', text: userQ });
    this.question = '';
    this.loading = true;

    this.api.askQuestion(this.projectId, userQ).subscribe({
      next: (res) => {
        this.loading = false;
        this.chatHistory.push({
          role: 'agent',
          text: res.answer || 'No answer provided by agent.'
        });
      },
      error: (err) => {
        this.loading = false;
        this.chatHistory.push({
          role: 'agent',
          text: `Error: ${err.message}`
        });
      }
    });
  }
}