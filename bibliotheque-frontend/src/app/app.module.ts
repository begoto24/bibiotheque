import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { AuthGuard } from './_auth/auth.guard';
import { AuthInterceptor } from './_auth/auth.interceptor';
import { ButtonComponent } from './_shared/button/button.component';
import { DashboardCalendarComponent } from './_shared/dashboard-calendar/dashboard-calendar.component';
import { SkeletonComponent } from './_shared/skeleton/skeleton.component';
import { ErrorHandlerService } from './_core/services/error-handler.service';
import { LoadingService } from './_core/services/loading.service';
import { UserAuthService } from './_service/user-auth.service';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ForbiddenComponent } from './forbidden/forbidden.component';
import { HeaderComponent } from './header/header.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { LogoutComponent } from './logout/logout.component';
import { TopbarComponent } from './topbar/topbar.component';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    HomeComponent,
    LoginComponent,
    LogoutComponent,
    ForbiddenComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    ButtonComponent,
    DashboardCalendarComponent,
    SkeletonComponent,
    TopbarComponent
  ],
  providers: [
    ErrorHandlerService,
    LoadingService,
    UserAuthService,
    AuthGuard,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
