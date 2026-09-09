import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Users } from '../../../_model/users';
import { SelectOption } from '../../../_shared/select/select.component';
import { UsersService } from '../../services/users.service';

@Component({
  selector: 'app-update-user',
  templateUrl: './update-user.component.html',
  styleUrls: ['./update-user.component.css']
})
export class UpdateUserComponent implements OnInit {

  userId: number;
  user: Users = new Users();
  submitting = false;
  error = '';
  readonly roleOptions: SelectOption[] = [
    { value: 'Admin', label: 'Admin' },
    { value: 'User', label: 'User' }
  ];

  constructor(private usersService: UsersService,
    private route: ActivatedRoute,
    private router: Router) { }

  ngOnInit(): void {
    this.userId = this.route.snapshot.params['userId'];
    this.usersService.getUserById(this.userId).subscribe({
      next: data => {
        this.user = data;
        // Defensif : si l'utilisateur arrive sans role (incoherence de
        // donnees cote back), le formulaire ([(ngModel)]="user.role[0]...")
        // ne doit pas planter au chargement.
        if (!this.user.role || !this.user.role[0]) {
          this.user.role = [{ roleName: 'User' }];
        }
      },
      error: error => this.error = error.message
    });
  }

  onSubmit() {
    this.submitting = true;
    this.error = '';
    this.usersService.updateUser(this.userId, this.user).subscribe({
      next: () => this.goToUsersList(),
      error: error => {
        this.submitting = false;
        this.error = error.message;
      }
    });
  }

  goToUsersList() {
    this.router.navigate(['/users']);
  }

}
