import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'Library Management System';

  /** true sur les pages "autonomes" (ex. connexion) : pas de sidebar/topbar. */
  hideShell = false;

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateShellVisibility());

    // Premier chargement : les événements de routage sont déjà passés.
    this.updateShellVisibility();
  }

  private updateShellVisibility(): void {
    let route = this.activatedRoute;
    while (route.firstChild) {
      route = route.firstChild;
    }
    this.hideShell = !!route.snapshot.data['standalone'];
  }
}
