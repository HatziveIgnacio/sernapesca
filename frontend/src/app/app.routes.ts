import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { MainLayout } from './layout/main-layout/main-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { Laboratorios } from './pages/laboratorios/laboratorios';
import { Plantillas } from './pages/plantillas/plantillas';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  {
    path: '',
    component: MainLayout,
    children: [
      { path: 'dashboard', component: Dashboard },
      { path: 'laboratorios', component: Laboratorios },
      { path: 'plantillas', component: Plantillas },
    ]
  }
];
