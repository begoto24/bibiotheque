import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from '../_auth/auth.guard';
import { BookDetailsComponent } from './components/book-details/book-details.component';
import { BooksListComponent } from './components/books-list/books-list.component';
import { UpdateBookComponent } from './components/update-book/update-book.component';

// Pas de route dédiée pour la création : <app-create-book> est embarqué
// directement dans BooksListComponent (bouton "+" -> modale), comme pour
// les réservations. Une URL /books/create n'existe donc plus.
const routes: Routes = [
  { path: '', component: BooksListComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'update/:bookId', component: UpdateBookComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } },
  { path: 'details/:bookId', component: BookDetailsComponent, canActivate: [AuthGuard], data: { roles: ['Admin'] } }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BooksRoutingModule { }
