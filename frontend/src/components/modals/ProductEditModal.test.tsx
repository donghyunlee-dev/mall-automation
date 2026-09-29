import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ProductEditModal from './ProductEditModal';
import type { GoodsItem } from '../../types/goods';
import type { CommonStatusConfig } from '../../types/commonStatusConfig';
import { updateGoods } from '../../api/goods';

vi.mock('../../api/goods', () => ({
  updateGoods: vi.fn(),
}));

const createItem = (erpSyncYn: GoodsItem['erpSyncYn']): GoodsItem => ({
  id: 1,
  godoGoodsCode: 'G001',
  erpGoodsCode: 'ERP001',
  goodsName: '테스트 상품',
  onlyMallYn: 'N',
  soldOutYn: 'N',
  availableQty: 10,
  safetyRate: 20,
  remainValidDays: 90,
  godoSyncYn: 'Y',
  erpSyncYn,
  godoSyncBlockedYn: 'N',
  message: null,
  status: 'NORMAL',
  createdId: 'tester@s-food.com',
  createdAt: '2026-04-02T00:00:00Z',
  updatedId: 'tester@s-food.com',
  updatedAt: '2026-04-02T00:00:00Z',
});

const statusConfigs: CommonStatusConfig[] = [
  { scope: 'GENERAL', outOfStockQty: 20, onSaleQty: 21, safetyRate: 50, remainValidDays: 150 },
  { scope: 'EXCLUSIVE', outOfStockQty: 1, onSaleQty: 2, safetyRate: 50, remainValidDays: 60 },
];

describe('ProductEditModal', () => {
  it('disables ERP code input when erpSyncYn is Y', () => {
    render(
      <ProductEditModal
        item={createItem('Y')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('ERP 품목코드 *')).toBeDisabled();
    expect(
      screen.getByText('ERP 연동이 완료된 품목은 코드 수정이 불가능해요.'),
    ).toBeInTheDocument();
  });

  it('keeps ERP code input enabled when erpSyncYn is N', () => {
    render(
      <ProductEditModal
        item={createItem('N')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('ERP 품목코드 *')).toBeEnabled();
    expect(
      screen.queryByText('ERP 연동이 완료된 품목은 코드 수정이 불가능해요.'),
    ).not.toBeInTheDocument();
  });

  it('shows config-derived criteria inputs as editable', () => {
    render(
      <ProductEditModal
        item={createItem('N')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('가용재고 기준')).toBeVisible();
    expect(screen.getByLabelText('가용재고 기준')).toBeEnabled();
    expect(screen.getByLabelText('잔여율 기준')).toBeVisible();
    expect(screen.getByLabelText('잔여율 기준')).toBeEnabled();
    expect(screen.getByLabelText('잔여일 기준')).toBeVisible();
    expect(screen.getByLabelText('잔여일 기준')).toBeEnabled();
  });

  it('sends item criteria in update body', async () => {
    const user = userEvent.setup();
    vi.mocked(updateGoods).mockResolvedValue(createItem('N'));

    render(
      <ProductEditModal
        item={createItem('N')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(updateGoods).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        availableQty: 10,
        safetyRate: 20,
        remainValidDays: 90,
      }),
    );
  });

  it('shows config reference text for the selected scope', () => {
    render(
      <ProductEditModal
        item={createItem('N')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    expect(
      screen.getByText('현재 기준: 품절 20 이하 / 판매 21 이상 / 잔여율 50% / 잔여일 150일'),
    ).toBeInTheDocument();
  });

  it('sends scope config values after scope change', async () => {
    const user = userEvent.setup();
    vi.mocked(updateGoods).mockResolvedValue(createItem('N'));

    render(
      <ProductEditModal
        item={createItem('N')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: '전용' }));
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(updateGoods).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        availableQty: 1,
        safetyRate: 50,
        remainValidDays: 60,
      }),
    );
  });

  it('saves auto-applied config values after scope change', async () => {
    const user = userEvent.setup();
    vi.mocked(updateGoods).mockResolvedValue(createItem('N'));

    render(
      <ProductEditModal
        item={createItem('N')}
        statusConfigs={statusConfigs}
        onClose={() => undefined}
        onSaved={() => undefined}
        onToast={vi.fn()}
      />,
    );

    await user.click(screen.getByRole('button', { name: '전용' }));
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(updateGoods).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        onlyMallYn: 'Y',
        availableQty: 1,
        safetyRate: 50,
        remainValidDays: 60,
      }),
    );
  });
});
