import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * Terme de la barre de recherche globale (header). Purement transport :
 * chaque page qui a une liste filtrable (Livres, Utilisateurs...) s'y
 * abonne et filtre ses propres données déjà chargées ; les pages qui n'ont
 * rien à filtrer l'ignorent simplement.
 */
@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private readonly querySubject = new BehaviorSubject<string>('');
  readonly query$ = this.querySubject.asObservable();

  setQuery(value: string): void {
    this.querySubject.next(value.trim().toLowerCase());
  }

  clear(): void {
    this.querySubject.next('');
  }
}
