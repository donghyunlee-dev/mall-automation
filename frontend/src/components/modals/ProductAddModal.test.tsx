import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ProductAddModal from './ProductAddModal';
import type { CommonStatusConfig } from '../../types/commonStatusConfig';

vi.mock('../../api/goods', () => ({
  createGoods: vi.fn(),
}));

const statusConfigs: CommonStatusConfig[] = [
  { scope: 'GENERAL', outOfStockQty: 20, onSaleQty: 21, safetyRate: 50, remainValidDays: 150 },
  { scope: 'EXCLUSIVE', outOfStockQty: 1, onSaleQty: 2, safetyRate: 50, remainValidDays: 60 },
];

describe('ProductAddModal', () => {
  it('shows config reference text for the default scope', () => {
    render(
      <ProductAddModal
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
});
