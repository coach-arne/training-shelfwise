import { createAction } from './async-action';

describe('createAction', () => {
  it('returns the result and clears loading', async () => {
    const action = createAction(async (value: number) => value * 2);
    expect(await action.run(21)).toBe(42);
    expect(action.loading()).toBeFalse();
    expect(action.error()).toBeNull();
  });

  it('stores the failure instead of throwing', async () => {
    const action = createAction(async () => {
      throw { status: 422, code: 'LOAN_LIMIT_REACHED', ruleId: 'BR-MEM-02', message: 'Limit.', details: [] };
    });
    expect(await action.run()).toBeUndefined();
    expect(action.error()?.code).toBe('LOAN_LIMIT_REACHED');
    action.clearError();
    expect(action.error()).toBeNull();
  });
});
