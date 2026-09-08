import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { SearchService } from '../../../_core/services/search.service';
import { Users } from '../../../_model/users';
import { UsersService } from '../../services/users.service';
import { RegistrationComponent } from '../registration/registration.component';

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit, OnDestroy {

  @ViewChild(RegistrationComponent) registrationForm!: RegistrationComponent;

  users: Users[] = [];
  filteredUsers: Users[] = [];
  loading = false;
  error = '';
  userPendingDeletion: Users | null = null;

  private searchQuery = '';
  private searchSubscription?: Subscription;

  constructor(private usersService: UsersService,
    private router: Router,
    private searchService: SearchService) { }

  ngOnInit(): void {
    this.getUsers();
    this.searchSubscription = this.searchService.query$.subscribe(query => {
      this.searchQuery = query;
      this.applyFilter();
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  getUsers() {
    this.loading = true;
    this.error = '';
    this.usersService.getUsers().pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: data => {
        this.users = data;
        this.applyFilter();
      },
      error: error => this.error = error.message
    });
  }

  private applyFilter(): void {
    if (!this.searchQuery) {
      this.filteredUsers = this.users;
      return;
    }
    this.filteredUsers = this.users.filter(user =>
      user.name?.toLowerCase().includes(this.searchQuery) ||
      user.username?.toLowerCase().includes(this.searchQuery)
    );
  }

  userDetails(userId: number) {
    this.router.navigate(['/users/details', userId ]);
  }

  updateUser(userId: number) {
    this.router.navigate(['/users/update', userId ]);
  }

  askDeleteUser(user: Users) {
    this.userPendingDeletion = user;
  }

  cancelDeleteUser() {
    this.userPendingDeletion = null;
  }

  confirmDeleteUser() {
    if (!this.userPendingDeletion) {
      return;
    }
    this.deleteUser(this.userPendingDeletion.userId);
  }

  deleteUser(userId: number) {
    this.usersService.deleteUser(userId).subscribe({
      next: () => {
        this.userPendingDeletion = null;
        this.getUsers();
      },
      error: error => {
        this.userPendingDeletion = null;
        this.error = error.message;
      }
    });
  }

}
