import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../../services/api';
import { ProjectState } from '../../services/project-state';

@Component({
  selector: 'app-file-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './file-inventory.html',
  styleUrl: './file-inventory.scss',
})
export class FileInventory implements OnInit {
  projectId: string | null = null;
  inventory: any[] = [];
  filteredInventory: any[] = [];
  searchTerm: string = '';
  selectedLanguage: string = 'ALL';
  languages: { name: string; count: number }[] = [];
  loading: boolean = false;
  totalNodes: number = 0;
  totalEdges: number = 0;

  constructor(private api: Api, private projectState: ProjectState) {}

  ngOnInit() {
    this.projectState.selectedProjectId$.subscribe((id) => {
      if (id) {
        this.projectId = id;
        this.loadInventory(id);
      }
    });
  }

  loadInventory(projectId: string) {
    this.loading = true;
    this.api.getProjectInventory(projectId).subscribe({
      next: (res) => {
        this.inventory = res.data || [];
        this.computeLanguageStats();
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });

    this.api.getProjectGraph(projectId).subscribe({
      next: (res) => {
        if (res.data) {
          this.totalNodes = res.data.nodes?.length || 0;
          this.totalEdges = res.data.relationships?.length || 0;
        }
      },
    });
  }

  computeLanguageStats() {
    const langMap = new Map<string, number>();
    for (const item of this.inventory) {
      const lang = item.detectedLanguage || 'unknown';
      langMap.set(lang, (langMap.get(lang) || 0) + 1);
    }
    this.languages = Array.from(langMap.entries()).map(([name, count]) => ({
      name,
      count,
    }));
  }

  applyFilter() {
    this.filteredInventory = this.inventory.filter((item) => {
      const matchesSearch =
        !this.searchTerm ||
        item.fileId.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        item.detectedLanguage.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesLang =
        this.selectedLanguage === 'ALL' || item.detectedLanguage === this.selectedLanguage;
      return matchesSearch && matchesLang;
    });
  }

  filterByLanguage(lang: string) {
    this.selectedLanguage = lang;
    this.applyFilter();
  }
}