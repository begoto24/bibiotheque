import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BadgeComponent } from '../_shared/badge/badge.component';
import { ButtonComponent } from '../_shared/button/button.component';
import { ConfirmDialogComponent } from '../_shared/confirm-dialog/confirm-dialog.component';
import { SelectComponent } from '../_shared/select/select.component';
import { TableStateComponent } from '../_shared/table-state/table-state.component';
import { ReservationRoutingModule } from './reservation-routing.module';
import { ReservationContainerComponent } from './components/reservation-container/reservation-container.component';
import { ReservationListComponent } from './components/reservation-list/reservation-list.component';
import { ReservationFormComponent } from './components/reservation-form/reservation-form.component';

@NgModule({
  declarations: [
    ReservationContainerComponent,
    ReservationListComponent,
    ReservationFormComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ReservationRoutingModule,
    ConfirmDialogComponent,
    ButtonComponent,
    BadgeComponent,
    TableStateComponent,
    SelectComponent
  ]
})
export class ReservationModule { }
