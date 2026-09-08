import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { UserAuthService } from '../_service/user-auth.service';
import { UsersService } from '../users/services/users.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private userAuthService: UserAuthService,
    private router: Router,
    private userService: UsersService
  ) {}
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    if(this.userAuthService.getToken() !== null) {
      const role = route.data["roles"] as Array<string>;

      // Pas de restriction de rôle sur cette route : tout utilisateur connecté
      // peut y accéder. (Avant ce correctif, l'absence de "roles" faisait
      // tomber dans le cas "non connecté" ci-dessous et renvoyait vers
      // /login même pour un utilisateur valide.)
      if(!role) {
        return true;
      }

      const match = this.userService.roleMatch(role);

      if(match) {
        return true;
      } else {
        this.router.navigate(['/forbidden']);
        return false;
      }
    }

    this.router.navigate(['/login']);
    return false;
  }
}
