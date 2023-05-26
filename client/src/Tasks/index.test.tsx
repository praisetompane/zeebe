/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

import {render, screen, fireEvent, waitFor} from 'modules/testing-library';
import {MemoryRouter} from 'react-router-dom';
import {MockThemeProvider} from 'modules/theme/MockProvider';
import {generateTask} from 'modules/mock-schema/mocks/tasks';
import {Tasks} from './index';
import {rest} from 'msw';
import {nodeMockServer} from 'modules/mockServer/nodeMockServer';
import {ReactQueryProvider} from 'modules/ReactQueryProvider';
import * as userMocks from 'modules/mock-schema/mocks/current-user';

const FIRST_PAGE = Array.from({length: 50}).map((_, index) =>
  generateTask(`${index}`),
);
const SECOND_PAGE = Array.from({length: 50}).map((_, index) =>
  generateTask(`${index + 50}`),
);

type Props = {
  children?: React.ReactNode;
};

const Wrapper: React.FC<Props> = ({children}) => {
  return (
    <ReactQueryProvider>
      <MemoryRouter initialEntries={['/']}>
        <MockThemeProvider>{children}</MockThemeProvider>
      </MemoryRouter>
    </ReactQueryProvider>
  );
};

describe('<Layout />', () => {
  it('should load more tasks', async () => {
    nodeMockServer.use(
      rest.get('/v1/internal/users/current', (_, res, ctx) => {
        return res.once(ctx.json(userMocks.currentUser));
      }),
      rest.post('/v1/tasks/search', async (req, res, ctx) => {
        const {searchAfter} = await req.json();
        if (searchAfter === undefined) {
          return res(ctx.json(FIRST_PAGE));
        }

        return res(ctx.json(SECOND_PAGE));
      }),
    );

    render(<Tasks />, {
      wrapper: Wrapper,
    });

    await waitFor(() => expect(screen.getByTitle('All open')).toBeDisabled());

    expect(await screen.findByText('TASK 0')).toBeInTheDocument();
    expect(screen.getByText('TASK 49')).toBeInTheDocument();
    expect(screen.getAllByRole('article')).toHaveLength(50);

    fireEvent.scroll(screen.getByTestId('scrollable-list'), {
      target: {scrollY: 100},
    });

    expect(screen.getByText('TASK 0')).toBeInTheDocument();
    expect(screen.getByText('TASK 49')).toBeInTheDocument();
    expect(await screen.findByText('TASK 50')).toBeInTheDocument();
    expect(screen.getByText('TASK 99')).toBeInTheDocument();
    expect(screen.getAllByRole('article')).toHaveLength(100);
  });
});
