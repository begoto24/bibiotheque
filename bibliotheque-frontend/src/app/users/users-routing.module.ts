import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../_auth/auth.guard';
import { UpdateUserComponent } from './components/update-user/update-user.component';
import { UserDetailsComponent } from './components/user-details/user-details.component';
import { UsersListComponent } from './components/users-list/users-list.component';

// Pas de route dédiée pour l'inscription : <app-registration> est embarqué
// directement dans UsersListComponent (bouton "+" -> modale), comme pour
// les réservations. Une URL /users/register n'existe donc plus.
const routes: Routes = [
  { path: '', component: UsersListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'details/:userId', component: UserDetailsComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'update/:userId', component: UpdateUserComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class UsersRoutingModule { }
