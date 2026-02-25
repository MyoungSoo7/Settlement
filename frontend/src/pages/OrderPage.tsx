import React, { useState } from 'react';
import { orderApi } from '@/api/order';
import { paymentApi } from '@/api/payment';
import { OrderResponse, PaymentResponse } from '@/types';
import Card from '@/components/Card';
import Spinner from '@/components/Spinner';

const OrderPage: React.FC = () => {
  const userId = 1; // 임시 사용자 ID
  const [amount, setAmount] = useState<string>('');
  const [paymentMethod, setPaymentMethod] = useState<string>('CARD');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [step, setStep] = useState<'input' | 'order-created' | 'payment-ready' | 'completed'>('input');

  // 주문 생성
  const handleCreateOrder = async () => {
    if (!amount || parseFloat(amount) <= 0) {
      setError('올바른 금액을 입력해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const orderResponse = await orderApi.createOrder({
        userId,
        amount: parseFloat(amount),
      });

      setOrder(orderResponse);
      setStep('order-created');
    } catch (err: any) {
      setError(err.response?.data?.message || '주문 생성에 실패했습니다.');
      console.error('Order creation error:', err);
    } finally {
      setLoading(false);
    }
  };

  // 결제 생성
  const handleCreatePayment = async () => {
    if (!order) return;

    setLoading(true);
    setError(null);

    try {
      const paymentResponse = await paymentApi.createPayment({
        orderId: order.id,
        paymentMethod,
      });

      setPayment(paymentResponse);
      setStep('payment-ready');
    } catch (err: any) {
      setError(err.response?.data?.message || '결제 생성에 실패했습니다.');
      console.error('Payment creation error:', err);
    } finally {
      setLoading(false);
    }
  };

  // 결제 승인
  const handleAuthorizePayment = async () => {
    if (!payment) return;

    setLoading(true);
    setError(null);

    try {
      const authorizedPayment = await paymentApi.authorizePayment(payment.id);
      setPayment(authorizedPayment);

      // 자동으로 결제 확정까지 진행
      setTimeout(() => handleCapturePayment(authorizedPayment.id), 1000);
    } catch (err: any) {
      setError(err.response?.data?.message || '결제 승인에 실패했습니다.');
      console.error('Payment authorization error:', err);
      setLoading(false);
    }
  };

  // 결제 확정
  const handleCapturePayment = async (paymentId: number) => {
    try {
      const capturedPayment = await paymentApi.capturePayment(paymentId);
      setPayment(capturedPayment);
      setStep('completed');
    } catch (err: any) {
      setError(err.response?.data?.message || '결제 확정에 실패했습니다.');
      console.error('Payment capture error:', err);
    } finally {
      setLoading(false);
    }
  };

  // 새로운 주문 시작
  const handleNewOrder = () => {
    setAmount('');
    setPaymentMethod('CARD');
    setOrder(null);
    setPayment(null);
    setError(null);
    setStep('input');
  };

  // 금액 포맷팅
  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
    }).format(value);
  };

  // 상태 뱃지
  const getStatusBadge = (status: string) => {
    const styles: Record<string, string> = {
      CREATED: 'bg-yellow-100 text-yellow-800',
      PAID: 'bg-green-100 text-green-800',
      CANCELED: 'bg-red-100 text-red-800',
      READY: 'bg-blue-100 text-blue-800',
      AUTHORIZED: 'bg-indigo-100 text-indigo-800',
      CAPTURED: 'bg-green-100 text-green-800',
    };
    return styles[status] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">주문 & 결제</h1>
          <p className="text-gray-600">상품 주문 및 결제를 진행합니다</p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
            <p className="text-red-800">{error}</p>
          </div>
        )}

        {/* Step 1: 주문 정보 입력 */}
        {step === 'input' && (
          <Card title="주문 정보 입력">
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  주문 금액 (원)
                </label>
                <input
                  type="number"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  placeholder="예: 50000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  min="0"
                  step="1000"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  결제 수단
                </label>
                <select
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                >
                  <option value="CARD">신용카드</option>
                  <option value="BANK_TRANSFER">계좌이체</option>
                  <option value="VIRTUAL_ACCOUNT">가상계좌</option>
                </select>
              </div>

              {loading ? (
                <Spinner size="md" message="주문 생성 중..." />
              ) : (
                <button
                  onClick={handleCreateOrder}
                  className="w-full bg-blue-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
                >
                  주문하기
                </button>
              )}
            </div>
          </Card>
        )}

        {/* Step 2: 주문 생성 완료 */}
        {step === 'order-created' && order && (
          <Card title="주문이 생성되었습니다">
            <div className="space-y-4 mb-6">
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">주문 번호</span>
                <span className="font-semibold">#{order.id}</span>
              </div>
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">주문 금액</span>
                <span className="font-semibold text-lg">{formatCurrency(order.amount)}</span>
              </div>
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">주문 상태</span>
                <span className={`px-3 py-1 rounded-full text-sm font-semibold ${getStatusBadge(order.status)}`}>
                  {order.status}
                </span>
              </div>
            </div>

            {loading ? (
              <Spinner size="md" message="결제 정보 생성 중..." />
            ) : (
              <button
                onClick={handleCreatePayment}
                className="w-full bg-blue-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
              >
                결제 진행하기
              </button>
            )}
          </Card>
        )}

        {/* Step 3: 결제 준비 완료 */}
        {step === 'payment-ready' && payment && (
          <Card title="결제 정보">
            <div className="space-y-4 mb-6">
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">결제 번호</span>
                <span className="font-semibold">#{payment.id}</span>
              </div>
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">결제 금액</span>
                <span className="font-semibold text-lg">{formatCurrency(payment.amount)}</span>
              </div>
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">결제 수단</span>
                <span className="font-semibold">{payment.paymentMethod}</span>
              </div>
              <div className="flex justify-between items-center py-3 border-b">
                <span className="text-gray-600">결제 상태</span>
                <span className={`px-3 py-1 rounded-full text-sm font-semibold ${getStatusBadge(payment.status)}`}>
                  {payment.status}
                </span>
              </div>
            </div>

            {loading ? (
              <Spinner size="md" message="결제 처리 중..." />
            ) : (
              <button
                onClick={handleAuthorizePayment}
                className="w-full bg-green-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-green-700 transition-colors"
              >
                결제하기
              </button>
            )}
          </Card>
        )}

        {/* Step 4: 결제 완료 */}
        {step === 'completed' && payment && order && (
          <Card>
            <div className="text-center mb-6">
              <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-4">
                <svg
                  className="h-10 w-10 text-green-600"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M5 13l4 4L19 7"
                  />
                </svg>
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">결제가 완료되었습니다!</h2>
              <p className="text-gray-600">정상적으로 결제가 처리되었습니다.</p>
            </div>

            <div className="bg-gray-50 rounded-lg p-6 mb-6">
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">주문 번호</span>
                  <span className="font-semibold">#{order.id}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">결제 번호</span>
                  <span className="font-semibold">#{payment.id}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">PG 거래 ID</span>
                  <span className="font-mono text-sm">{payment.pgTransactionId}</span>
                </div>
                <div className="flex justify-between items-center pt-3 border-t border-gray-300">
                  <span className="text-gray-900 font-medium">결제 금액</span>
                  <span className="text-2xl font-bold text-green-600">{formatCurrency(payment.amount)}</span>
                </div>
              </div>
            </div>

            <button
              onClick={handleNewOrder}
              className="w-full bg-blue-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
            >
              새로운 주문하기
            </button>
          </Card>
        )}
      </div>
    </div>
  );
};

export default OrderPage;
