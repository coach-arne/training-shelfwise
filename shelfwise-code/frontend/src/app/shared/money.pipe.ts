import { Pipe, PipeTransform } from '@angular/core';
import { formatMoney } from '../helpers/format';

@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatMoney(value);
  }
}
