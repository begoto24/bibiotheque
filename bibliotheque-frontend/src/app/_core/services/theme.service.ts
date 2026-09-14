import { Injectable } from '@angular/core';

type Theme = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'bibliotheque-theme';

/**
 * Bascule clair/sombre. Purement une préférence d'affichage côté client :
 * aucun appel serveur, aucun lien avec l'authentification ou les données
 * métier. Persisté en localStorage (par navigateur, pas par compte) ; tant
 * qu'aucun choix explicite n'a été fait, on suit prefers-color-scheme (géré
 * en CSS, cf. styles.css) sans rien poser sur <html> ni en localStorage.
 */
@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private theme: Theme = 'system';

  constructor() {
    this.theme = this.readStoredTheme();
    this.apply();
  }

  isDark(): boolean {
    if (this.theme === 'system') {
      return this.systemPrefersDark();
    }
    return this.theme === 'dark';
  }

  toggle(): void {
    this.theme = this.isDark() ? 'light' : 'dark';
    this.apply();
    try {
      localStorage.setItem(STORAGE_KEY, this.theme);
    } catch {
      // Navigation privée / stockage bloqué : le thème reste actif pour
      // cette session, simplement pas mémorisé pour la prochaine visite.
    }
  }

  private readStoredTheme(): Theme {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'light' || stored === 'dark') {
        return stored;
      }
    } catch {
      // idem : pas de mémoire disponible, on retombe sur "system".
    }
    return 'system';
  }

  private systemPrefersDark(): boolean {
    return typeof window !== 'undefined'
      && !!window.matchMedia
      && window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  private apply(): void {
    const root = document.documentElement;
    if (this.theme === 'system') {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', this.theme);
    }
  }
}
