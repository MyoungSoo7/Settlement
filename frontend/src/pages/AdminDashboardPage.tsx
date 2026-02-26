import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { adminApi, AdminUserResponse } from '@/api/admin';
import { productApi } from '@/api/product';
import { orderApi } from '@/api/order';
import { couponApi } from '@/api/coupon';
import { OrderResponse, ProductResponse, CouponResponse, CouponType, CouponCreateRequest } from '@/types';
import Spinner from '@/components/Spinner';

type Tab = 'overview' | 'orders' | 'products' | 'users' | 'coupons';

const fmt = (v: number) =>
  new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW' }).format(v);

const fmtDate = (s: string) =>
  new Date(s).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' });

// ── 통계 카드 ────────────────────────────────────────────────────────────
const StatCard: React.FC<{
  label: string; value: string | number; sub?: string;
  icon: React.ReactNode; color: string;
}> = ({ label, value, sub, icon, color }) => (
  <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
    <div className="flex items-center justify-between mb-3">
      <span className="text-sm font-medium text-gray-500">{label}</span>
      <div className={`w-10 h-10 rounded-lg ${color} flex items-center justify-center`}>
        {icon}
      </div>
    </div>
    <p className="text-2xl font-bold text-gray-900">{value}</p>
    {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
  </div>
);

// ── 상태 배지 ─────────────────────────────────────────────────────────────
const OrderStatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const cfg: Record<string, string> = {
    CREATED:  'bg-yellow-100 text-yellow-800',
    PAID:     'bg-green-100 text-green-800',
    CANCELED: 'bg-red-100 text-red-800',
    REFUNDED: 'bg-purple-100 text-purple-800',
  };
  const label: Record<string, string> = {
    CREATED: '주문완료', PAID: '결제완료', CANCELED: '취소됨', REFUNDED: '환불됨',
  };
  return (
    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${cfg[status] ?? 'bg-gray-100 text-gray-700'}`}>
      {label[status] ?? status}
    </span>
  );
};

const ProductStatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const cfg: Record<string, string> = {
    ACTIVE:       'bg-green-100 text-green-800',
    INACTIVE:     'bg-gray-100 text-gray-700',
    OUT_OF_STOCK: 'bg-orange-100 text-orange-800',
    DISCONTINUED: 'bg-red-100 text-red-800',
  };
  const label: Record<string, string> = {
    ACTIVE: '판매중', INACTIVE: '비활성', OUT_OF_STOCK: '품절', DISCONTINUED: '단종',
  };
  return (
    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${cfg[status] ?? 'bg-gray-100 text-gray-700'}`}>
      {label[status] ?? status}
    </span>
  );
};

const RoleBadge: React.FC<{ role: string }> = ({ role }) => {
  const cfg: Record<string, string> = {
    ADMIN:   'bg-red-100 text-red-800',
    MANAGER: 'bg-purple-100 text-purple-800',
    USER:    'bg-blue-100 text-blue-800',
  };
  return (
    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${cfg[role] ?? 'bg-gray-100 text-gray-700'}`}>
      {role}
    </span>
  );
};

// ════════════════════════════════════════════════════════════════════════════
const AdminDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<Tab>('overview');

  const [orders,   setOrders]   = useState<OrderResponse[]>([]);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [users,    setUsers]    = useState<AdminUserResponse[]>([]);
  const [coupons,  setCoupons]  = useState<CouponResponse[]>([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState<string | null>(null);

  // 쿠폰 생성 폼
  const [couponForm, setCouponForm] = useState<CouponCreateRequest>({
    code: '', type: 'PERCENTAGE', discountValue: 10, minOrderAmount: 0, maxUses: 100,
  });
  const [couponError, setCouponError]   = useState<string | null>(null);
  const [couponSuccess, setCouponSuccess] = useState<string | null>(null);
  const [creatingCoupon, setCreatingCoupon] = useState(false);

  // 필터
  const [orderStatusFilter,  setOrderStatusFilter]  = useState('ALL');
  const [productStatusFilter, setProductStatusFilter] = useState('ALL');
  const [userRoleFilter,      setUserRoleFilter]      = useState('ALL');
  const [orderSearch,  setOrderSearch]  = useState('');
  const [productSearch, setProductSearch] = useState('');
  const [userSearch,    setUserSearch]    = useState('');

  // 주문 취소 처리
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const [orderList, productList, userList, couponList] = await Promise.all([
          adminApi.getAllOrders(),
          productApi.getAllProducts(),
          adminApi.getAllUsers(),
          couponApi.getAll(),
        ]);
        setOrders(orderList.sort((a, b) => b.id - a.id));
        setProducts(productList.sort((a, b) => b.id - a.id));
        setUsers(userList.sort((a, b) => a.id - b.id));
        setCoupons(couponList.sort((a, b) => b.id - a.id));
      } catch {
        setError('데이터를 불러오지 못했습니다.');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  // ── 통계 계산 ──
  const stats = useMemo(() => {
    const totalRevenue   = orders.filter((o) => o.status === 'PAID').reduce((s, o) => s + o.amount, 0);
    const paidCount      = orders.filter((o) => o.status === 'PAID').length;
    const createdCount   = orders.filter((o) => o.status === 'CREATED').length;
    const canceledCount  = orders.filter((o) => o.status === 'CANCELED').length;
    const lowStockCount  = products.filter((p) => p.stockQuantity > 0 && p.stockQuantity < 10).length;
    const outOfStockCount = products.filter((p) => p.stockQuantity === 0).length;
    return { totalRevenue, paidCount, createdCount, canceledCount, lowStockCount, outOfStockCount };
  }, [orders, products]);

  // ── 필터된 목록 ──
  const filteredOrders = useMemo(() =>
    orders.filter((o) =>
      (orderStatusFilter === 'ALL' || o.status === orderStatusFilter) &&
      (orderSearch === '' || String(o.id).includes(orderSearch) || String(o.userId).includes(orderSearch))
    ), [orders, orderStatusFilter, orderSearch]);

  const filteredProducts = useMemo(() =>
    products.filter((p) =>
      (productStatusFilter === 'ALL' || p.status === productStatusFilter) &&
      (productSearch === '' || p.name.toLowerCase().includes(productSearch.toLowerCase()))
    ), [products, productStatusFilter, productSearch]);

  const filteredUsers = useMemo(() =>
    users.filter((u) =>
      (userRoleFilter === 'ALL' || u.role === userRoleFilter) &&
      (userSearch === '' || u.email.toLowerCase().includes(userSearch.toLowerCase()))
    ), [users, userRoleFilter, userSearch]);

  const handleCancelOrder = async (orderId: number) => {
    if (!window.confirm(`주문 #${orderId}을 취소하시겠습니까?`)) return;
    setCancellingId(orderId);
    try {
      const updated = await orderApi.cancelOrder(orderId);
      setOrders((prev) => prev.map((o) => (o.id === orderId ? updated : o)));
    } catch {
      alert('주문 취소에 실패했습니다.');
    } finally {
      setCancellingId(null);
    }
  };

  const handleCreateCoupon = async (e: React.FormEvent) => {
    e.preventDefault();
    setCouponError(null);
    setCouponSuccess(null);
    setCreatingCoupon(true);
    try {
      const created = await couponApi.create(couponForm);
      setCoupons((prev) => [created, ...prev]);
      setCouponSuccess(`쿠폰 "${created.code}" 생성 완료!`);
      setCouponForm({ code: '', type: 'PERCENTAGE', discountValue: 10, minOrderAmount: 0, maxUses: 100 });
    } catch (err: any) {
      setCouponError(err.response?.data?.message || '쿠폰 생성에 실패했습니다.');
    } finally {
      setCreatingCoupon(false);
    }
  };

  const TABS: { id: Tab; label: string; icon: string }[] = [
    { id: 'overview',  label: '개요',      icon: '📊' },
    { id: 'orders',    label: '주문 관리',  icon: '📦' },
    { id: 'products',  label: '상품 관리',  icon: '🛍️' },
    { id: 'users',     label: '회원 관리',  icon: '👥' },
    { id: 'coupons',   label: '쿠폰 관리',  icon: '🎟️' },
  ];

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Spinner size="lg" message="관리자 데이터 로드 중..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <p className="text-red-600">{error}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">

        {/* 헤더 */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">관리자 대시보드</h1>
            <p className="text-sm text-gray-500 mt-1">
              주문 {orders.length}건 · 상품 {products.length}개 · 회원 {users.length}명
            </p>
          </div>
          <div className="flex gap-2">
            <Link to="/product"
              className="px-4 py-2 bg-blue-600 text-white text-sm font-semibold rounded-lg hover:bg-blue-700 transition-colors">
              상품 관리 페이지 →
            </Link>
            <Link to="/admin/settlement"
              className="px-4 py-2 bg-purple-600 text-white text-sm font-semibold rounded-lg hover:bg-purple-700 transition-colors">
              정산 관리 →
            </Link>
            <Link to="/dashboard"
              className="px-4 py-2 bg-indigo-600 text-white text-sm font-semibold rounded-lg hover:bg-indigo-700 transition-colors">
              정산 대시보드 →
            </Link>
          </div>
        </div>

        {/* 탭 */}
        <div className="flex bg-white rounded-xl shadow-sm border border-gray-200 mb-6 p-1 gap-1">
          {TABS.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex-1 py-2.5 text-sm font-semibold rounded-lg transition-all ${
                activeTab === tab.id
                  ? 'bg-gray-900 text-white shadow'
                  : 'text-gray-500 hover:text-gray-800 hover:bg-gray-50'
              }`}
            >
              {tab.icon} {tab.label}
            </button>
          ))}
        </div>

        {/* ── 개요 탭 ─────────────────────────────────────────────────────── */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* 통계 카드 */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              <StatCard
                label="총 주문"
                value={orders.length.toLocaleString()}
                sub={`결제완료 ${stats.paidCount}건`}
                color="bg-blue-50"
                icon={<svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"/></svg>}
              />
              <StatCard
                label="총 매출"
                value={fmt(stats.totalRevenue)}
                sub="결제 완료 기준"
                color="bg-green-50"
                icon={<svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>}
              />
              <StatCard
                label="총 회원"
                value={users.length.toLocaleString()}
                sub={`관리자 ${users.filter(u => u.role === 'ADMIN').length}명`}
                color="bg-purple-50"
                icon={<svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0"/></svg>}
              />
              <StatCard
                label="총 상품"
                value={products.length.toLocaleString()}
                sub={`재고부족 ${stats.lowStockCount}개 · 품절 ${stats.outOfStockCount}개`}
                color="bg-orange-50"
                icon={<svg className="w-5 h-5 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>}
              />
            </div>

            {/* 주문 상태 분포 */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="bg-white rounded-xl border border-gray-200 p-5">
                <h3 className="font-bold text-gray-900 mb-4">주문 상태 현황</h3>
                <div className="space-y-3">
                  {[
                    { status: 'CREATED',  label: '주문완료',  cls: 'bg-yellow-400', count: stats.createdCount },
                    { status: 'PAID',     label: '결제완료',  cls: 'bg-green-400',  count: stats.paidCount },
                    { status: 'CANCELED', label: '취소됨',    cls: 'bg-red-400',    count: orders.filter(o=>o.status==='CANCELED').length },
                    { status: 'REFUNDED', label: '환불됨',    cls: 'bg-purple-400', count: orders.filter(o=>o.status==='REFUNDED').length },
                  ].map(({ label, cls, count }) => (
                    <div key={label} className="flex items-center gap-3">
                      <div className={`w-3 h-3 rounded-full ${cls}`} />
                      <span className="text-sm text-gray-600 w-20">{label}</span>
                      <div className="flex-1 bg-gray-100 rounded-full h-2">
                        <div
                          className={`${cls} h-2 rounded-full transition-all`}
                          style={{ width: orders.length ? `${(count / orders.length) * 100}%` : '0%' }}
                        />
                      </div>
                      <span className="text-sm font-semibold text-gray-800 w-12 text-right">{count}건</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* 최근 주문 5건 */}
              <div className="bg-white rounded-xl border border-gray-200 p-5">
                <h3 className="font-bold text-gray-900 mb-4">최근 주문</h3>
                <div className="space-y-2">
                  {orders.slice(0, 5).map((order) => (
                    <div key={order.id} className="flex items-center justify-between py-1.5">
                      <div>
                        <p className="text-sm font-medium text-gray-900">주문 #{order.id}</p>
                        <p className="text-xs text-gray-400">사용자 #{order.userId} · {fmtDate(order.createdAt)}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <OrderStatusBadge status={order.status} />
                        <span className="text-sm font-bold text-gray-800">{fmt(order.amount)}</span>
                      </div>
                    </div>
                  ))}
                </div>
                <button onClick={() => setActiveTab('orders')}
                  className="w-full mt-3 text-center text-sm text-blue-600 hover:text-blue-800 font-medium py-2 rounded-lg hover:bg-blue-50 transition-colors">
                  전체 주문 보기 →
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ── 주문 관리 탭 ─────────────────────────────────────────────────── */}
        {activeTab === 'orders' && (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            {/* 필터 바 */}
            <div className="p-4 border-b border-gray-100 flex flex-wrap gap-3 items-center">
              <input
                type="text"
                placeholder="주문ID / 회원ID 검색"
                value={orderSearch}
                onChange={(e) => setOrderSearch(e.target.value)}
                className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm w-44 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <div className="flex gap-1.5">
                {['ALL', 'CREATED', 'PAID', 'CANCELED', 'REFUNDED'].map((s) => (
                  <button
                    key={s}
                    onClick={() => setOrderStatusFilter(s)}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                      orderStatusFilter === s
                        ? 'bg-gray-900 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {s === 'ALL' ? '전체' : { CREATED:'주문완료', PAID:'결제완료', CANCELED:'취소됨', REFUNDED:'환불됨' }[s]}
                  </button>
                ))}
              </div>
              <span className="text-sm text-gray-400 ml-auto">{filteredOrders.length}건</span>
            </div>

            {/* 테이블 */}
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    {['주문 ID', '회원 ID', '상품 ID', '금액', '상태', '주문일', ''].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {filteredOrders.slice(0, 100).map((order) => (
                    <tr key={order.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 font-mono text-gray-700">#{order.id}</td>
                      <td className="px-4 py-3 text-gray-600">#{order.userId}</td>
                      <td className="px-4 py-3 text-gray-600">#{order.productId ?? '-'}</td>
                      <td className="px-4 py-3 font-semibold text-gray-900">{fmt(order.amount)}</td>
                      <td className="px-4 py-3"><OrderStatusBadge status={order.status} /></td>
                      <td className="px-4 py-3 text-gray-400">{fmtDate(order.createdAt)}</td>
                      <td className="px-4 py-3">
                        {order.status === 'CREATED' && (
                          <button
                            onClick={() => handleCancelOrder(order.id)}
                            disabled={cancellingId === order.id}
                            className="text-xs text-red-500 hover:text-red-700 font-medium disabled:opacity-40"
                          >
                            {cancellingId === order.id ? '처리중...' : '취소'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredOrders.length > 100 && (
                <p className="text-center text-sm text-gray-400 py-3">100건만 표시 중 (전체 {filteredOrders.length}건)</p>
              )}
              {filteredOrders.length === 0 && (
                <p className="text-center text-gray-400 py-10">조건에 맞는 주문이 없습니다.</p>
              )}
            </div>
          </div>
        )}

        {/* ── 상품 관리 탭 ─────────────────────────────────────────────────── */}
        {activeTab === 'products' && (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="p-4 border-b border-gray-100 flex flex-wrap gap-3 items-center">
              <input
                type="text"
                placeholder="상품명 검색"
                value={productSearch}
                onChange={(e) => setProductSearch(e.target.value)}
                className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm w-44 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <div className="flex gap-1.5">
                {['ALL', 'ACTIVE', 'INACTIVE', 'OUT_OF_STOCK', 'DISCONTINUED'].map((s) => (
                  <button
                    key={s}
                    onClick={() => setProductStatusFilter(s)}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                      productStatusFilter === s
                        ? 'bg-gray-900 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {s === 'ALL' ? '전체' : { ACTIVE:'판매중', INACTIVE:'비활성', OUT_OF_STOCK:'품절', DISCONTINUED:'단종' }[s]}
                  </button>
                ))}
              </div>
              <span className="text-sm text-gray-400 ml-auto">{filteredProducts.length}개</span>
              <Link to="/product"
                className="px-3 py-1.5 bg-blue-600 text-white text-xs font-semibold rounded-lg hover:bg-blue-700 transition-colors">
                상세 관리 →
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    {['ID', '이미지', '상품명', '가격', '재고', '상태', '등록일'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {filteredProducts.map((product) => (
                    <tr key={product.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 font-mono text-gray-500">#{product.id}</td>
                      <td className="px-4 py-3">
                        {product.primaryImageUrl ? (
                          <img src={product.primaryImageUrl} alt={product.name}
                            className="w-9 h-9 rounded-lg object-cover" />
                        ) : (
                          <div className="w-9 h-9 rounded-lg bg-gray-100 flex items-center justify-center">
                            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5"
                                d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                            </svg>
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-gray-900">{product.name}</p>
                        {product.description && (
                          <p className="text-xs text-gray-400 truncate max-w-[200px]">{product.description}</p>
                        )}
                      </td>
                      <td className="px-4 py-3 font-semibold text-gray-900">{fmt(product.price)}</td>
                      <td className="px-4 py-3">
                        <span className={`font-semibold ${
                          product.stockQuantity === 0 ? 'text-red-600'
                          : product.stockQuantity < 10 ? 'text-orange-500'
                          : 'text-gray-800'
                        }`}>
                          {product.stockQuantity}
                        </span>
                      </td>
                      <td className="px-4 py-3"><ProductStatusBadge status={product.status} /></td>
                      <td className="px-4 py-3 text-gray-400">{fmtDate(product.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredProducts.length === 0 && (
                <p className="text-center text-gray-400 py-10">조건에 맞는 상품이 없습니다.</p>
              )}
            </div>
          </div>
        )}

        {/* ── 회원 관리 탭 ─────────────────────────────────────────────────── */}
        {activeTab === 'users' && (
          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div className="p-4 border-b border-gray-100 flex flex-wrap gap-3 items-center">
              <input
                type="text"
                placeholder="이메일 검색"
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm w-52 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <div className="flex gap-1.5">
                {['ALL', 'USER', 'ADMIN', 'MANAGER'].map((r) => (
                  <button
                    key={r}
                    onClick={() => setUserRoleFilter(r)}
                    className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors ${
                      userRoleFilter === r
                        ? 'bg-gray-900 text-white'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {r === 'ALL' ? '전체' : r}
                  </button>
                ))}
              </div>
              <span className="text-sm text-gray-400 ml-auto">{filteredUsers.length}명</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    {['ID', '이메일', '역할', '가입일'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
                        {h}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {filteredUsers.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-4 py-3 font-mono text-gray-500">#{user.id}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center flex-shrink-0">
                            <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                            </svg>
                          </div>
                          <span className="font-medium text-gray-900">{user.email}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3"><RoleBadge role={user.role} /></td>
                      <td className="px-4 py-3 text-gray-400">{fmtDate(user.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredUsers.length === 0 && (
                <p className="text-center text-gray-400 py-10">조건에 맞는 회원이 없습니다.</p>
              )}
            </div>
          </div>
        )}

        {/* ── 쿠폰 관리 탭 ─────────────────────────────────────────────────── */}
        {activeTab === 'coupons' && (
          <div className="space-y-6">
            {/* 쿠폰 생성 폼 */}
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h3 className="font-bold text-gray-900 mb-4">쿠폰 생성</h3>
              <form onSubmit={handleCreateCoupon} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">쿠폰 코드</label>
                  <input
                    type="text"
                    required
                    value={couponForm.code}
                    onChange={(e) => setCouponForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))}
                    placeholder="예: SUMMER20"
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">쿠폰 타입</label>
                  <select
                    value={couponForm.type}
                    onChange={(e) => setCouponForm((f) => ({ ...f, type: e.target.value as CouponType }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="PERCENTAGE">정률 할인 (%)</option>
                    <option value="FIXED">정액 할인 (원)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    할인 값 ({couponForm.type === 'PERCENTAGE' ? '%' : '원'})
                  </label>
                  <input
                    type="number"
                    required
                    min={1}
                    max={couponForm.type === 'PERCENTAGE' ? 100 : undefined}
                    value={couponForm.discountValue}
                    onChange={(e) => setCouponForm((f) => ({ ...f, discountValue: Number(e.target.value) }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">최소 주문 금액 (원)</label>
                  <input
                    type="number"
                    min={0}
                    value={couponForm.minOrderAmount}
                    onChange={(e) => setCouponForm((f) => ({ ...f, minOrderAmount: Number(e.target.value) }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">최대 사용 횟수</label>
                  <input
                    type="number"
                    required
                    min={1}
                    value={couponForm.maxUses}
                    onChange={(e) => setCouponForm((f) => ({ ...f, maxUses: Number(e.target.value) }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-gray-600 mb-1">만료일 (선택)</label>
                  <input
                    type="datetime-local"
                    value={couponForm.expiresAt ?? ''}
                    onChange={(e) => setCouponForm((f) => ({ ...f, expiresAt: e.target.value || undefined }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500"
                  />
                </div>
                <div className="md:col-span-2 lg:col-span-3 flex items-center gap-4">
                  <button
                    type="submit"
                    disabled={creatingCoupon}
                    className="px-6 py-2.5 bg-blue-600 text-white text-sm font-semibold rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
                  >
                    {creatingCoupon ? '생성 중...' : '쿠폰 생성'}
                  </button>
                  {couponSuccess && <p className="text-sm text-green-600 font-medium">{couponSuccess}</p>}
                  {couponError   && <p className="text-sm text-red-600">{couponError}</p>}
                </div>
              </form>
            </div>

            {/* 쿠폰 목록 */}
            <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
              <div className="p-4 border-b border-gray-100 flex items-center justify-between">
                <h3 className="font-bold text-gray-900">쿠폰 목록</h3>
                <span className="text-sm text-gray-400">{coupons.length}개</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      {['코드', '타입', '할인', '최소금액', '사용', '만료일', '상태'].map((h) => (
                        <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {coupons.map((c) => (
                      <tr key={c.id} className="hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 font-mono font-bold text-blue-700">{c.code}</td>
                        <td className="px-4 py-3">
                          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                            c.type === 'PERCENTAGE' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'
                          }`}>
                            {c.type === 'PERCENTAGE' ? '정률' : '정액'}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-semibold text-gray-900">
                          {c.type === 'PERCENTAGE' ? `${c.discountValue}%` : fmt(c.discountValue)}
                        </td>
                        <td className="px-4 py-3 text-gray-600">
                          {c.minOrderAmount > 0 ? fmt(c.minOrderAmount) : '-'}
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-gray-700">{c.usedCount}</span>
                          <span className="text-gray-400"> / {c.maxUses}</span>
                        </td>
                        <td className="px-4 py-3 text-gray-400">
                          {c.expiresAt ? new Date(c.expiresAt).toLocaleDateString('ko-KR') : '무기한'}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                            c.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'
                          }`}>
                            {c.isActive ? '활성' : '비활성'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {coupons.length === 0 && (
                  <p className="text-center text-gray-400 py-10">쿠폰이 없습니다. 위 폼에서 생성하세요.</p>
                )}
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
};

export default AdminDashboardPage;
