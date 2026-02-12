import api from './axios';
import { SettlementSearchRequest, SettlementSearchResponse, SettlementDetail } from '@/types';

export const settlementApi = {
  /**
   * 정산 복합 검색 (GET)
   * GET /api/settlements/search
   */
  search: async (params: SettlementSearchRequest): Promise<SettlementSearchResponse> => {
    const response = await api.get<SettlementSearchResponse>('/api/settlements/search', {
      params,
    });
    return response.data;
  },

  /**
   * 정산 복합 검색 (POST)
   * POST /api/settlements/search
   */
  searchByPost: async (request: SettlementSearchRequest): Promise<SettlementSearchResponse> => {
    const response = await api.post<SettlementSearchResponse>('/api/settlements/search', request);
    return response.data;
  },

  /**
   * 정산 상세 조회
   * GET /api/settlements/{id}
   */
  getSettlement: async (id: number): Promise<SettlementDetail> => {
    const response = await api.get<SettlementDetail>(`/api/settlements/${id}`);
    return response.data;
  },

  /**
   * 정산 승인
   * POST /api/settlements/{id}/approve
   */
  approveSettlement: async (id: number): Promise<SettlementDetail> => {
    const response = await api.post<SettlementDetail>(`/api/settlements/${id}/approve`);
    return response.data;
  },

  /**
   * 정산 반려
   * POST /api/settlements/{id}/reject
   */
  rejectSettlement: async (id: number, reason: string): Promise<SettlementDetail> => {
    const response = await api.post<SettlementDetail>(`/api/settlements/${id}/reject`, {
      reason,
    });
    return response.data;
  },
};
