import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SiteFooterComponent } from '../../../../shared/components/site-footer/site-footer.component';
import { SiteHeaderComponent } from '../../../../shared/components/site-header/site-header.component';

interface HomeProductItem {
  id: string;
  name: string;
  price: number;
  oldPrice?: number;
  imageUrl: string;
  badge?: string;
  specs?: string[];
}

interface QuickActionItem {
  title: string;
  subtitle: string;
  link: string;
}

interface BundleDealItem {
  title: string;
  description: string;
  discountText: string;
  link: string;
}

interface CategoryHighlightItem {
  name: string;
  countLabel: string;
  imageUrl: string;
  link: string;
}

interface UtilityFeatureItem {
  title: string;
  description: string;
}

interface LiveStatItem {
  value: string;
  label: string;
}

interface FlashDeal extends HomeProductItem {
  link: string;
}

interface ProductTab {
  label: string;
  items: HomeProductItem[];
}

interface ProductCluster {
  title: string;
  ctaLabel: string;
  ctaLink: string;
  tabs: ProductTab[];
}

interface ArticleItem {
  title: string;
  date: string;
  category: 'TIN TUC' | 'REVIEW';
  link: string;
}

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink, SiteHeaderComponent, SiteFooterComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss',
})
export class HomePageComponent implements OnInit, OnDestroy {
  private countdownTimer?: ReturnType<typeof setInterval>;

  readonly flashEndsAt = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000 + 6 * 60 * 60 * 1000);

  readonly flashDeals: FlashDeal[] = [
    {
      id: 'fs-1',
      name: 'LHC Apex 16 RTX Edition',
      price: 32990000,
      oldPrice: 36990000,
      imageUrl:
        'https://images.unsplash.com/photo-1593642634443-44adaa06623a?auto=format&fit=crop&w=1200&q=80',
      badge: 'FLASH -11%',
      link: '/shop/products',
      specs: ['i7 14650HX', '16GB DDR5', 'RTX 5060', '16 inch 240Hz'],
    },
    {
      id: 'fs-2',
      name: 'LHC Phantom Pro Wireless 8K',
      price: 2590000,
      oldPrice: 3190000,
      imageUrl:
        'https://images.unsplash.com/photo-1563297007-0686b7003af7?auto=format&fit=crop&w=1200&q=80',
      badge: 'FLASH -19%',
      link: '/shop/products',
      specs: ['8K Polling', '59g', 'PAW 3395', '70h pin'],
    },
  ];

  readonly mockData: HomeProductItem[] = [
    {
      id: 'feat-1',
      name: 'LHC Phantom X Pro Wireless',
      price: 2890000,
      imageUrl:
        'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=1000&q=80',
      badge: 'Best Seller',
      specs: ['26K DPI', '4K Polling', '72h pin'],
    },
    {
      id: 'feat-2',
      name: 'LHC Velocity TKL Optical',
      price: 2490000,
      imageUrl:
        'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=1000&q=80',
      badge: 'Hot',
      specs: ['Optical switch', 'TKL', 'Rapid Trigger'],
    },
    {
      id: 'feat-3',
      name: 'LHC NovaSound 7.1 Headset',
      price: 3190000,
      imageUrl:
        'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=1000&q=80',
      specs: ['7.1 Surround', '50mm driver', 'Mic AI ENC'],
    },
    {
      id: 'feat-4',
      name: 'LHC GlideXL RGB Pad',
      price: 790000,
      imageUrl:
        'https://images.unsplash.com/photo-1640955014216-75201056c829?auto=format&fit=crop&w=1000&q=80',
      specs: ['900x400mm', 'Speed surface', 'RGB Edge'],
    },
  ];

  readonly productClusters: ProductCluster[] = [
    {
      title: 'SẢN PHẨM MỚI',
      ctaLabel: 'Xem tất cả',
      ctaLink: '/shop/products',
      tabs: [
        {
          label: 'Laptop',
          items: [
            {
              id: 'new-lap-1',
              name: 'LHC Titan 15 OLED',
              price: 35990000,
              imageUrl:
                'https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=1000&q=80',
              specs: ['Ultra 9 285H', '32GB DDR5', 'RTX 5070', '15.6 OLED 240Hz'],
            },
            {
              id: 'new-lap-2',
              name: 'LHC Nova 14 AI',
              price: 28990000,
              imageUrl:
                'https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?auto=format&fit=crop&w=1000&q=80',
              specs: ['Ryzen AI 9', '24GB LPDDR5X', 'Radeon 890M', '14 inch 165Hz'],
            },
          ],
        },
        {
          label: 'Bàn phím',
          items: [
            {
              id: 'new-kb-1',
              name: 'LHC Surge 75 HE',
              price: 3290000,
              imageUrl:
                'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=1000&q=80',
              specs: ['Hall Effect', '8K Polling', 'Hot-swap'],
            },
            {
              id: 'new-kb-2',
              name: 'LHC Forge 98 Pro',
              price: 2690000,
              imageUrl:
                'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=1000&q=80',
              specs: ['Gasket mount', 'Tri-mode', 'PBT doubleshot'],
            },
          ],
        },
      ],
    },
    {
      title: 'SẢN PHẨM BÁN CHẠY',
      ctaLabel: 'Top bán chạy',
      ctaLink: '/shop/products',
      tabs: [
        {
          label: 'Chuot',
          items: [
            {
              id: 'best-m-1',
              name: 'LHC Phantom X Pro Wireless',
              price: 2890000,
              imageUrl:
                'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=1000&q=80',
              badge: 'Best Seller',
              specs: ['26K DPI', '4K Polling', '72h pin'],
            },
            {
              id: 'best-m-2',
              name: 'LHC Drift Mini 8K',
              price: 1990000,
              imageUrl:
                'https://images.unsplash.com/photo-1625842268584-8f3296236761?auto=format&fit=crop&w=1000&q=80',
              specs: ['8K Polling', '49g', 'PTFE feet'],
            },
          ],
        },
        {
          label: 'Tai nghe',
          items: [
            {
              id: 'best-h-1',
              name: 'LHC NovaSound 7.1 Headset',
              price: 3190000,
              imageUrl:
                'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=1000&q=80',
              specs: ['50mm driver', '7.1 Surround', 'Mic AI ENC'],
            },
            {
              id: 'best-h-2',
              name: 'LHC Echo Air Wireless',
              price: 2290000,
              imageUrl:
                'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1000&q=80',
              specs: ['Low latency', '70h battery', 'Dual mode'],
            },
          ],
        },
      ],
    },
    {
      title: 'GAMING GEAR',
      ctaLabel: 'Duyet danh muc',
      ctaLink: '/shop/products',
      tabs: [
        {
          label: 'Setup FPS',
          items: [
            {
              id: 'fps-1',
              name: 'LHC Velocity TKL + Phantom X',
              price: 5190000,
              imageUrl:
                'https://images.unsplash.com/photo-1603302576837-37561b2e2302?auto=format&fit=crop&w=1000&q=80',
              badge: 'Combo',
              specs: ['Rapid Trigger', '4K Polling', 'Esports tuned'],
            },
            {
              id: 'fps-2',
              name: 'LHC Focus XL + GlideXL',
              price: 3390000,
              imageUrl:
                'https://images.unsplash.com/photo-1640955014216-75201056c829?auto=format&fit=crop&w=1000&q=80',
              specs: ['Wave 7.1', 'Mic clear', 'XL control pad'],
            },
          ],
        },
        {
          label: 'Streaming',
          items: [
            {
              id: 'st-1',
              name: 'LHC VoiceCast Pro Kit',
              price: 4790000,
              imageUrl:
                'https://images.unsplash.com/photo-1590602847861-f357a9332bbc?auto=format&fit=crop&w=1000&q=80',
              specs: ['USB-C mic', 'RGB arm', 'Monitoring jack'],
            },
            {
              id: 'st-2',
              name: 'LHC Stream Deck Mini+',
              price: 2990000,
              imageUrl:
                'https://images.unsplash.com/photo-1587202372775-e229f172b9d7?auto=format&fit=crop&w=1000&q=80',
              specs: ['12 keys', 'Macro scenes', 'OBS ready'],
            },
          ],
        },
      ],
    },
  ];

  readonly activeClusterTab = [0, 0, 0];

  readonly articles: ArticleItem[] = [
    {
      title: 'Hướng dẫn chọn chuột gaming 2026 theo grip và FPS title',
      date: '03/04/2026',
      category: 'TIN TUC',
      link: '/shop/products',
    },
    {
      title: 'Review LHC Surge 75 HE: rapid trigger co thuc su khac biet?',
      date: '02/04/2026',
      category: 'REVIEW',
      link: '/shop/products',
    },
    {
      title: '5 preset audio cho Valorant, CS2 va Apex de nghe chan tot hon',
      date: '31/03/2026',
      category: 'TIN TUC',
      link: '/shop/products',
    },
    {
      title: 'So sanh sensor 3395 vs 3950: do chenh lech co dang nang cap?',
      date: '29/03/2026',
      category: 'REVIEW',
      link: '/shop/products',
    },
  ];

  countdown = {
    days: '00',
    hours: '00',
    minutes: '00',
    seconds: '00',
  };

  readonly quickActions: QuickActionItem[] = [
    {
      title: 'Build PC Corner',
      subtitle: 'Chọn nhanh combo theo ngân sách',
      link: '/shop/products',
    },
    {
      title: 'Esports Setup',
      subtitle: 'Top gear cho FPS cạnh tranh',
      link: '/shop/products',
    },
    {
      title: 'Streaming Room',
      subtitle: 'Mic, headset, lighting mượt',
      link: '/shop/products',
    },
  ];

  readonly bundleDeals: BundleDealItem[] = [
    {
      title: 'Combo Prowl X + Velocity TKL',
      description: 'Chuột + bàn phím optical low-latency dành cho ranker.',
      discountText: '-15%',
      link: '/shop/products',
    },
    {
      title: 'NovaSound 7.1 + GlideXL RGB',
      description: 'Âm thanh chiến thuật kèm mousepad diện tích lớn.',
      discountText: '-10%',
      link: '/shop/products',
    },
  ];

  readonly categoryHighlights: CategoryHighlightItem[] = [
    {
      name: 'Gaming Mouse',
      countLabel: '18 mẫu mới',
      imageUrl:
        'https://images.unsplash.com/photo-1563297007-0686b7003af7?auto=format&fit=crop&w=900&q=80',
      link: '/shop/products',
    },
    {
      name: 'Mechanical Keyboard',
      countLabel: '26 phiên bản',
      imageUrl:
        'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=900&q=80',
      link: '/shop/products',
    },
    {
      name: 'Headset / Audio',
      countLabel: '12 dòng flagship',
      imageUrl:
        'https://images.unsplash.com/photo-1546435770-a3e426bf472b?auto=format&fit=crop&w=900&q=80',
      link: '/shop/products',
    },
  ];

  readonly utilityFeatures: UtilityFeatureItem[] = [
    {
      title: 'Giao nhanh 2H nội thành',
      description: 'Đặt trước 18h, nhận trong ngày tại khu vực hỗ trợ.',
    },
    {
      title: 'Đổi mới 30 ngày',
      description: 'Lỗi nhà sản xuất đổi mới nhanh, không chờ đợi lâu.',
    },
    {
      title: 'Hỗ trợ kỹ thuật 24/7',
      description: 'Tư vấn setup, tuning DPI, macro và tối ưu hiệu năng.',
    },
    {
      title: 'Thanh toán linh hoạt',
      description: 'COD, thẻ, chuyển khoản và trả góp 0% qua đối tác.',
    },
  ];

  readonly liveStats: LiveStatItem[] = [
    { value: '12K+', label: 'Khách hàng hài lòng' },
    { value: '98.6%', label: 'Đơn giao đúng hẹn' },
    { value: '4.9/5', label: 'Đánh giá trung bình' },
    { value: '36H', label: 'Xử lý bảo hành trung bình' },
  ];

  ngOnInit(): void {
    this.updateCountdown();
    this.countdownTimer = setInterval(() => this.updateCountdown(), 1000);
  }

  ngOnDestroy(): void {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
    }
  }

  setActiveTab(clusterIndex: number, tabIndex: number): void {
    this.activeClusterTab[clusterIndex] = tabIndex;
  }

  getVisibleClusterItems(clusterIndex: number): HomeProductItem[] {
    return this.productClusters[clusterIndex].tabs[this.activeClusterTab[clusterIndex]].items;
  }

  private updateCountdown(): void {
    const diff = this.flashEndsAt.getTime() - Date.now();
    if (diff <= 0) {
      this.countdown = { days: '00', hours: '00', minutes: '00', seconds: '00' };
      return;
    }

    const totalSeconds = Math.floor(diff / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    this.countdown = {
      days: String(days).padStart(2, '0'),
      hours: String(hours).padStart(2, '0'),
      minutes: String(minutes).padStart(2, '0'),
      seconds: String(seconds).padStart(2, '0'),
    };
  }

  formatPrice(value: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(value);
  }
}
