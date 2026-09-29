import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ProductTable from './ProductTable';
import type { GoodsItem } from '../../types/goods';

const createItem = (overrides: Partial<GoodsItem> = {}): GoodsItem => ({
  id: 1,
  godoGoodsCode: '1000000504',
  erpGoodsCode: '171101109Z',
  goodsName: '테스트 상품',
  onlyMallYn: 'N',
  soldOutYn: 'N',
  availableQty: 20,
  safetyRate: 40,
  remainValidDays: 150,
  godoSyncYn: 'Y',
  erpSyncYn: 'Y',
  godoSyncBlockedYn: 'N',
  message: null,
  status: 'NORMAL',
  createdId: 'tester@s-food.com',
  createdAt: '2026-04-06T00:00:00Z',
  updatedId: 'tester@s-food.com',
  updatedAt: '2026-04-06T00:00:00Z',
  ...overrides,
});

describe('ProductTable', () => {
  it('renders blocked label button for validated blocked goods', () => {
    render(
      <ProductTable
        items={[createItem({ godoSyncBlockedYn: 'Y' })]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={() => undefined}
        onForceGodoStatusSync={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '차단' })).toBeEnabled();
  });

  it('shows unavailable label button for unverified goods', () => {
    render(
      <ProductTable
        items={[createItem({ status: 'UNVERIFIED' })]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={() => undefined}
        onForceGodoStatusSync={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '불가' })).toBeDisabled();
  });

  it('calls toggle handler when status button is clicked', async () => {
    const user = userEvent.setup();
    const onToggleGodoSyncBlock = vi.fn();

    render(
      <ProductTable
        items={[createItem()]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={onToggleGodoSyncBlock}
        onForceGodoStatusSync={() => undefined}
      />,
    );

    await user.click(screen.getByRole('button', { name: '허용' }));

    expect(onToggleGodoSyncBlock).toHaveBeenCalledTimes(1);
    expect(onToggleGodoSyncBlock).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }));
  });

  it('calls force sync handler when godo-synced on-sale status chip is clicked', async () => {
    const user = userEvent.setup();
    const onForceGodoStatusSync = vi.fn();

    render(
      <ProductTable
        items={[createItem({ soldOutYn: 'N', erpSyncYn: 'N', godoSyncYn: 'Y' })]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={() => undefined}
        onForceGodoStatusSync={onForceGodoStatusSync}
      />,
    );

    await user.click(screen.getByRole('button', { name: '판매중' }));

    expect(onForceGodoStatusSync).toHaveBeenCalledTimes(1);
    expect(onForceGodoStatusSync).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }));
  });

  it('does not enable force sync for blocked goods', () => {
    render(
      <ProductTable
        items={[createItem({ godoSyncBlockedYn: 'Y' })]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={() => undefined}
        onForceGodoStatusSync={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '판매중' })).toBeDisabled();
  });

  it('does not enable force sync for unavailable sale status', () => {
    render(
      <ProductTable
        items={[createItem({ godoSyncYn: 'N' })]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={() => undefined}
        onForceGodoStatusSync={() => undefined}
      />,
    );

    expect(screen.getByRole('button', { name: '확인불가' })).toBeDisabled();
  });

  it('enables force sync even when goods status is not NORMAL', async () => {
    const user = userEvent.setup();
    const onForceGodoStatusSync = vi.fn();

    render(
      <ProductTable
        items={[createItem({ status: 'ERROR', soldOutYn: 'N', godoSyncYn: 'Y', godoSyncBlockedYn: 'N' })]}
        loading={false}
        error=""
        sortKey="updatedAt"
        sortDir="desc"
        page={1}
        pageSize={10}
        totalPages={1}
        onSort={() => undefined}
        onPage={() => undefined}
        onEdit={() => undefined}
        onDelete={() => undefined}
        onToggleGodoSyncBlock={() => undefined}
        onForceGodoStatusSync={onForceGodoStatusSync}
      />,
    );

    await user.click(screen.getByRole('button', { name: '판매중' }));

    expect(onForceGodoStatusSync).toHaveBeenCalledTimes(1);
    expect(onForceGodoStatusSync).toHaveBeenCalledWith(expect.objectContaining({ id: 1 }));
  });
});
