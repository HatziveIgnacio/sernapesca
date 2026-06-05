import { Routes } from '@angular/router';
import { Login } from './auth/login/login';
import { MainLayout } from './layout/main-layout/main-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { Laboratorios } from './pages/laboratorios/laboratorios';
import { Plantillas } from './pages/plantillas/plantillas';
import { Placeholder } from './pages/placeholder/placeholder';

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
      {
        path: 'registros',
        component: Placeholder,
        data: {
          title: 'Registros',
          description: 'El módulo de registros aún no está implementado en el frontend Angular.'
        }
      },
      {
        path: 'validacion',
        component: Placeholder,
        data: {
          title: 'Validación',
          description: 'El flujo de validación detallada está pendiente de implementación.'
        }
      },
      {
        path: 'reportes',
        component: Placeholder,
        data: {
          title: 'Reportes',
          description: 'Los reportes operativos se incorporarán en una siguiente etapa.'
        }
      },
      {
        path: 'soporte',
        component: Placeholder,
        data: {
          title: 'Soporte',
          description: 'La sección de soporte institucional todavía no tiene una vista dedicada.'
        }
      },
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
