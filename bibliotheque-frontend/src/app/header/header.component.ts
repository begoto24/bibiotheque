import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { UserAuthService } from '../_service/user-auth.service';

const RAIL_STORAGE_KEY = 'sidebarRailCollapsed';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  /** Tiroir mobile fermé (< 992px) — sans lien avec le repli desktop ci-dessous. */
  isCollapsed = true;

  /** Sidebar repliée en rail icônes seules (>= 992px), mémorisé entre les sessions. */
  isRailCollapsed = localStorage.getItem(RAIL_STORAGE_KEY) === 'true';

  constructor(
    private userAuthService: UserAuthService,
    private router: Router
  ) { }

  toggleMenu(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  closeMenu(): void {
    this.isCollapsed = true;
  }

  toggleRail(): void {
    this.isRailCollapsed = !this.isRailCollapsed;
    localStorage.setItem(RAIL_STORAGE_KEY, String(this.isRailCollapsed));
  }

  isLoggedIn(): boolean {
    return !!this.userAuthService.isLoggedIn();
  }

  isAdmin(): boolean {
    return this.userAuthService.isAdmin();
  }

  getUserName(): string {
    return this.userAuthService.getName() || 'Utilisateur';
  }

  logout(): void {
    this.userAuthService.clear();
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}
