import { effect, WritableSignal } from '@angular/core';
import { consumePickedBarcode, pickedBarcode } from '../store/training-kit.store';

/** Call in a component's field initialiser: a barcode chosen in the training kit is put into `target`. */
export function fillFromPickedBarcode(target: WritableSignal<string>): void {
  effect(() => {
    const barcode = pickedBarcode();
    if (barcode) {
      target.set(barcode);
      consumePickedBarcode();
    }
  });
}
