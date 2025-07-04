import { Pact } from '@pact-foundation/pact';
import path from 'path';

describe('Product Service', () => {
  let provider;

  beforeAll(() => {
    provider = new Pact({
      consumer: 'Frontend',
      provider: 'Backend',
      port: 8081,
      log: path.resolve(process.cwd(), 'logs', 'pact.log'),
      dir: path.resolve(process.cwd(), 'pacts'),
      logLevel: 'INFO',
    });
    return provider.setup();
  });

  afterAll(() => {
    return provider.finalize();
  });

  describe('get all products', () => {
    beforeEach(() => {
      return provider.addInteraction({
        state: 'i have a list of products',
        uponReceiving: 'a request for all products',
        withRequest: {
          method: 'GET',
          path: '/api/products',
        },
        willRespondWith: {
          status: 200,
          headers: { 'Content-Type': 'application/json;charset=UTF-8' },
          body: [
            {
              id: '1',
              name: 'Laptop',
              price: 1200.00,
            },
          ],
        },
      });
    });

    it('should return a list of products', () => {
      return fetch('http://localhost:8081/api/products').then(response => {
        expect(response.status).toBe(200);
      });
    });
  });
});
