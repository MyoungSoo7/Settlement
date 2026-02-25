import React, { useState } from 'react';
import CreateProductForm from '@/components/product/CreateProductForm';
import ProductList from '@/components/product/ProductList';
import { ProductResponse } from '@/types';

const ProductPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'list' | 'create'>('list');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [selectedProduct, setSelectedProduct] = useState<ProductResponse | null>(null);

  const handleProductCreated = () => {
    setRefreshTrigger(prev => prev + 1);
    setActiveTab('list');
  };

  const handleProductSelect = (product: ProductResponse) => {
    setSelectedProduct(product);
    // 상품 상세 보기 모달을 여기에 구현할 수 있습니다
    console.log('선택된 상품:', product);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 헤더 */}
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <h1 className="text-3xl font-bold text-gray-900">상품 관리</h1>
          <p className="mt-2 text-sm text-gray-600">
            상품을 등록하고 관리할 수 있습니다.
          </p>
        </div>
      </div>

      {/* 탭 네비게이션 */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mt-6">
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('list')}
              className={`${
                activeTab === 'list'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200`}
            >
              📦 상품 목록
            </button>
            <button
              onClick={() => setActiveTab('create')}
              className={`${
                activeTab === 'create'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200`}
            >
              ➕ 상품 등록
            </button>
          </nav>
        </div>
      </div>

      {/* 컨텐츠 */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'list' && (
          <div>
            <div className="mb-6 flex justify-between items-center">
              <div>
                <h2 className="text-xl font-semibold text-gray-900">상품 목록</h2>
                <p className="mt-1 text-sm text-gray-600">
                  등록된 모든 상품을 확인하고 관리할 수 있습니다.
                </p>
              </div>
              <button
                onClick={() => setActiveTab('create')}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors duration-200 font-medium"
              >
                + 새 상품 등록
              </button>
            </div>
            <ProductList
              onProductSelect={handleProductSelect}
              refreshTrigger={refreshTrigger}
            />
          </div>
        )}

        {activeTab === 'create' && (
          <div>
            <CreateProductForm
              onSuccess={handleProductCreated}
              onCancel={() => setActiveTab('list')}
            />
          </div>
        )}
      </div>

      {/* 상품 상세 모달 (선택사항) */}
      {selectedProduct && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
          onClick={() => setSelectedProduct(null)}
        >
          <div
            className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6">
              <div className="flex justify-between items-start mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                  {selectedProduct.name}
                </h2>
                <button
                  onClick={() => setSelectedProduct(null)}
                  className="text-gray-400 hover:text-gray-600 text-2xl"
                >
                  ×
                </button>
              </div>

              <div className="space-y-4">
                <div>
                  <h3 className="text-sm font-semibold text-gray-700 mb-1">상품 ID</h3>
                  <p className="text-gray-900">{selectedProduct.id}</p>
                </div>

                {selectedProduct.description && (
                  <div>
                    <h3 className="text-sm font-semibold text-gray-700 mb-1">설명</h3>
                    <p className="text-gray-900">{selectedProduct.description}</p>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <h3 className="text-sm font-semibold text-gray-700 mb-1">가격</h3>
                    <p className="text-xl font-bold text-blue-600">
                      {new Intl.NumberFormat('ko-KR', {
                        style: 'currency',
                        currency: 'KRW',
                      }).format(selectedProduct.price)}
                    </p>
                  </div>

                  <div>
                    <h3 className="text-sm font-semibold text-gray-700 mb-1">재고</h3>
                    <p className="text-xl font-bold text-gray-900">
                      {selectedProduct.stockQuantity}개
                    </p>
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-semibold text-gray-700 mb-1">상태</h3>
                  <p className="text-gray-900">{selectedProduct.status}</p>
                </div>

                <div>
                  <h3 className="text-sm font-semibold text-gray-700 mb-1">
                    판매 가능 여부
                  </h3>
                  <p className="text-gray-900">
                    {selectedProduct.availableForSale ? '가능' : '불가능'}
                  </p>
                </div>

                <div className="pt-4 border-t">
                  <div className="text-sm text-gray-500 space-y-1">
                    <p>
                      등록일:{' '}
                      {new Date(selectedProduct.createdAt).toLocaleString('ko-KR')}
                    </p>
                    <p>
                      수정일:{' '}
                      {new Date(selectedProduct.updatedAt).toLocaleString('ko-KR')}
                    </p>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex justify-end">
                <button
                  onClick={() => setSelectedProduct(null)}
                  className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors duration-200"
                >
                  닫기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductPage;
