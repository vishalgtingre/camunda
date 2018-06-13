import {logout, user} from './api';

import {mockResolvedAsyncFn} from 'modules/testUtils';

import * as wrappers from 'modules/request/wrappers';

wrappers.get = mockResolvedAsyncFn();

describe('header api', () => {
  describe('logout', () => {
    it('should call post with the right url', () => {
      // given
      wrappers.post = mockResolvedAsyncFn();

      // when
      logout();

      // then
      expect(wrappers.post.mock.calls[0][0]).toBe('/logout');
    });
  });

  describe('user', () => {
    it('should call get with the right url', async () => {
      // given
      const successMessage = 'success';
      const successResponse = {
        json: mockResolvedAsyncFn(successMessage)
      };
      wrappers.get = mockResolvedAsyncFn(successResponse);

      // when
      const response = await user();

      // then
      expect(wrappers.get.mock.calls[0][0]).toBe('/authentications/user');
      expect(successResponse.json).toBeCalled();
      expect(response).toEqual(successMessage);
    });
  });
});
