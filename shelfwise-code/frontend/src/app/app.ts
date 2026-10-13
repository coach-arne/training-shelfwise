import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Shell } from './layout/shell';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Shell],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<app-shell><router-outlet /></app-shell>`,
})
export class App {}
