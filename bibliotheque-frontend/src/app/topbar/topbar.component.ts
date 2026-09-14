import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { SearchService } from '../_core/services/search.service';
import { UserAuthService } from '../_service/user-auth.service';

/**
 * Barre du haut (desktop uniquement, >= 992px) : recherche + profil/déconnexion.
 * Sur mobile, le profil/déconnexion reste dans le tiroir de la sidebar
 * (app-header) pour ne pas dupliquer ces actions sur deux endroits à la fois.
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.css']
})
export class TopbarComponent implements OnInit, OnDestroy {
  searchQuery = '';
  /** Le bouton Déconnexion n'est plus affiché en permanence : il vit dans ce
   *  menu, ouvert en cliquant sur le profil (moins de bruit visuel constant). */
  isProfileMenuOpen = false;

  private routerSubscription?: Subscription;

  constructor(
    private userAuthService: UserAuthService,
    private router: Router,
    private searchService: SearchService,
    private elementRef: ElementRef<HTMLElement>
  ) { }

  ngOnInit(): void {
    // Vide la recherche à chaque navigation : un terme tapé sur "Livres"
    // n'a plus de sens une fois sur "Réservations".
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.searchQuery = '';
        this.searchService.clear();
        this.isProfileMenuOpen = false;
      });
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
  }

  toggleProfileMenu(): void {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.isProfileMenuOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.isProfileMenuOpen = false;
  }

  onSearchInput(value: string): void {
    this.searchQuery = value;
    this.searchService.setQuery(value);
  }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  getUserName(): string {
    return this.userAuthService.getName() || 'Utilisateur';
  }

  logout(): void {
    this.userAuthService.clear();
    this.router.navigate(['/login']);
  }
}
