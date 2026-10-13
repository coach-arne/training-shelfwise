import { DestroyRef, inject } from '@angular/core';

export interface DebouncedSearch {
  /** Drop a pending timer without running the search. */
  cancelPending: () => void;
  /**
   * Call before an immediate search that also updates the watched signals
   * (for example setting the query from `?q=`), so a pending debounce does not run.
   */
  suppressNext: () => void;
  /**
   * Call after updating a watched source from the template (for example ngModelChange).
   * Schedules {@code run} after {@code delayMs} of quiet.
   */
  notify: () => void;
}

/**
 * Debounces {@code run} when search inputs change.
 * Call {@link DebouncedSearch.notify} from input handlers after updating the signals that
 * {@code sources} would read. The first paint does not search (nothing calls notify yet).
 * Must be called in an injection context (for example a component constructor).
 */
export function watchDebouncedSearch(
  _sources: () => unknown,
  run: () => void,
  delayMs = 300,
): DebouncedSearch {
  const destroyRef = inject(DestroyRef);
  let timer: ReturnType<typeof setTimeout> | null = null;
  let skipNext = false;

  const cancelPending = (): void => {
    if (timer !== null) {
      clearTimeout(timer);
      timer = null;
    }
  };

  const suppressNext = (): void => {
    cancelPending();
    skipNext = true;
  };

  const notify = (): void => {
    if (skipNext) {
      skipNext = false;
      return;
    }
    cancelPending();
    timer = setTimeout(() => {
      timer = null;
      run();
    }, delayMs);
  };

  destroyRef.onDestroy(cancelPending);

  return { cancelPending, suppressNext, notify };
}
