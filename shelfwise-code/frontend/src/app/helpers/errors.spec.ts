import { HttpErrorResponse } from '@angular/common/http';
import { errorText, toApiFailure } from './errors';

describe('error helpers', () => {
  it('reads the error payload of a refused action (HTTP 422)', () => {
    const failure = toApiFailure(new HttpErrorResponse({
      status: 422,
      error: { code: 'MEMBER_BLOCKED', ruleId: 'BR-MEM-03', message: 'The member is blocked.', details: [] },
    }));
    expect(failure.code).toBe('MEMBER_BLOCKED');
    expect(failure.ruleId).toBe('BR-MEM-03');
    expect(errorText(failure)).toBe('The member is blocked.');
  });

  it('gives a readable message when the server cannot be reached', () => {
    const failure = toApiFailure(new HttpErrorResponse({ status: 0 }));
    expect(failure.code).toBe('NETWORK');
  });

  it('keeps an existing failure unchanged', () => {
    const existing = { status: 403, code: 'FORBIDDEN', ruleId: null, message: 'No.', details: [] };
    expect(toApiFailure(existing)).toBe(existing);
  });
});
