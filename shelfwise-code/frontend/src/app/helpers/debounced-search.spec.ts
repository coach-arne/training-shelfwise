import { Injector, runInInjectionContext, signal } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DebouncedSearch, watchDebouncedSearch } from './debounced-search';

describe('watchDebouncedSearch', () => {
  function start(sources: () => unknown, run: () => void, delayMs = 50): DebouncedSearch {
    return runInInjectionContext(TestBed.inject(Injector), () => watchDebouncedSearch(sources, run, delayMs));
  }

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('does not run until notified, then runs after the delay', fakeAsync(() => {
    const query = signal('');
    const runs: string[] = [];
    const auto = start(() => query(), () => runs.push(query()), 50);
    tick(50);
    expect(runs).toEqual([]);

    query.set('a');
    auto.notify();
    tick(49);
    expect(runs).toEqual([]);
    tick(1);
    expect(runs).toEqual(['a']);
  }));

  it('resets the timer when notified again', fakeAsync(() => {
    const query = signal('a');
    const runs: string[] = [];
    const auto = start(() => query(), () => runs.push(query()), 50);

    query.set('ab');
    auto.notify();
    tick(30);
    query.set('abc');
    auto.notify();
    tick(50);
    expect(runs).toEqual(['abc']);
  }));

  it('cancelPending prevents a scheduled run', fakeAsync(() => {
    const query = signal('a');
    const runs: string[] = [];
    const auto = start(() => query(), () => runs.push(query()), 50);

    query.set('ab');
    auto.notify();
    auto.cancelPending();
    tick(50);
    expect(runs).toEqual([]);
  }));

  it('suppressNext skips the next notify after an immediate update', fakeAsync(() => {
    const query = signal('');
    const runs: string[] = [];
    const auto = start(() => query(), () => runs.push(query()), 50);

    auto.suppressNext();
    query.set('lighthouse');
    auto.notify();
    tick(50);
    expect(runs).toEqual([]);

    query.set('other');
    auto.notify();
    tick(50);
    expect(runs).toEqual(['other']);
  }));
});
