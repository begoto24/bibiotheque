import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { BadgeComponent } from '../_shared/badge/badge.component';
import { ButtonComponent } from '../_shared/button/button.component';
import { ConfirmDialogComponent } from '../_shared/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '../_shared/form-field/form-field.component';
import { SelectComponent } from '../_shared/select/select.component';
import { TableStateComponent } from '../_shared/table-state/table-state.component';
import { RegistrationComponent } from './components/registration/registration.component';
import { UpdateUserComponent } from './components/update-user/update-user.component';
import { UserDetailsComponent } from './components/user-details/user-details.component';
import { UsersListComponent } from './components/users-list/users-list.component';
import { UsersRoutingModule } from './users-routing.module';

@NgModule({
  declarations: [
    UsersListComponent,
    UserDetailsComponent,
    UpdateUserComponent,
    RegistrationComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    UsersRoutingModule,
    ConfirmDialogComponent,
    ButtonComponent,
    BadgeComponent,
    FormFieldComponent,
    TableStateComponent,
    SelectComponent
  ]
})
export class UsersModule { }
