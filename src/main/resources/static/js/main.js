/**
 * Bakery Order and Custom Cake Booking Platform
 * Section 1: Hero Canvas Scroll Animation (108 frames)
 * Section 2: Opposing Horizontal Gallery Rows
 * Section 3: Dual-Track Interactive Cards
 */

(function () {
    'use strict';

    /* ==========================================================================
       SECTION 1: HERO SCROLL ANIMATION
       ========================================================================== */
    const FRAME_COUNT = 108;
    const heroSection = document.getElementById('heroScrollSection');
    const heroStickyContainer = document.getElementById('heroStickyContainer');
    const heroLeftCol = document.getElementById('heroLeftCol');
    const heroCanvas = document.getElementById('heroCanvas');
    const heroLoaderBar = document.getElementById('heroLoaderBar');
    const heroScrollIndicator = document.getElementById('heroScrollIndicator');
    const heroPromoCard1 = document.getElementById('heroPromoCard1');
    const heroPromoCard2 = document.getElementById('heroPromoCard2');

    if (heroCanvas && heroSection) {
        const ctx = heroCanvas.getContext('2d');
        const images = [];
        let loadedCount = 0;
        let currentFrameIndex = 0;
        let isFirstRendered = false;

        // Path generator for existing animation frames in folder '1-Hero Section/left side hero animation/'
        function getFramePath(index) {
            const padded = String(index).padStart(3, '0');
            return `/images/hero/animation/ezgif-frame-${padded}.jpg`;
        }

        // Synchronize right-side promo card with current hero frame:
        // Frames 1–63 (index 0 to 62) -> Card 1 (Bakery Items)
        // Frame 64 onward (index 63 to 107) -> Card 2 (Custom Cake Booking)
        function updateRightPanel(frameIdx) {
            if (frameIdx < 63) {
                if (heroPromoCard1 && !heroPromoCard1.classList.contains('is-active')) {
                    heroPromoCard1.classList.add('is-active');
                }
                if (heroPromoCard2 && heroPromoCard2.classList.contains('is-active')) {
                    heroPromoCard2.classList.remove('is-active');
                }
            } else {
                if (heroPromoCard1 && heroPromoCard1.classList.contains('is-active')) {
                    heroPromoCard1.classList.remove('is-active');
                }
                if (heroPromoCard2 && !heroPromoCard2.classList.contains('is-active')) {
                    heroPromoCard2.classList.add('is-active');
                }
            }
        }

        // Adjust canvas internal dimensions to match device pixel ratio and CSS client size of left column
        function resizeCanvas() {
            const dpr = window.devicePixelRatio || 1;
            const leftColEl = document.getElementById('heroLeftCol') || heroLeftCol;
            const availableWidth = leftColEl ? leftColEl.clientWidth : (heroCanvas.clientWidth || window.innerWidth * 0.7);
            const availableHeight = leftColEl ? leftColEl.clientHeight : (heroCanvas.clientHeight || (window.innerHeight - 76));

            heroCanvas.width = Math.round(availableWidth * dpr);
            heroCanvas.height = Math.round(availableHeight * dpr);

            renderFrame(currentFrameIndex);
        }

        // Draws the original frame using contain scaling inside the left 70% hero area
        function renderFrame(index) {
            let img = images[index];
            if (!img || !img.complete || img.naturalWidth === 0) {
                for (let d = 1; d < FRAME_COUNT; d++) {
                    if (index - d >= 0 && images[index - d] && images[index - d].complete && images[index - d].naturalWidth > 0) {
                        img = images[index - d];
                        break;
                    }
                    if (index + d < FRAME_COUNT && images[index + d] && images[index + d].complete && images[index + d].naturalWidth > 0) {
                        img = images[index + d];
                        break;
                    }
                }
            }
            if (!img || !img.complete || img.naturalWidth === 0) {
                ctx.setTransform(1, 0, 0, 1, 0, 0);
                ctx.fillStyle = '#200402';ctx.fillRect(0,0,heroCanvas.width,heroCanvas.height);
                ctx.fillStyle = '#f5eadc';ctx.textAlign='center';ctx.font=`${Math.max(24,heroCanvas.width/15)}px Georgia`;
                ctx.fillText('Sweet Bloom',heroCanvas.width/2,heroCanvas.height/2);
                updateRightPanel(index);return;
            }

            const dpr = window.devicePixelRatio || 1;
            const leftColEl = document.getElementById('heroLeftCol') || heroLeftCol;
            const availableWidth = leftColEl ? leftColEl.clientWidth : (heroCanvas.width / dpr);
            const availableHeight = leftColEl ? leftColEl.clientHeight : (heroCanvas.height / dpr);

            // Reset transform and clear entire canvas buffer
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            ctx.clearRect(0, 0, heroCanvas.width, heroCanvas.height);

            // Fill background with warm dark red bakery radial gradient
            const bgGrad = ctx.createRadialGradient(
                heroCanvas.width / 2, heroCanvas.height / 2, heroCanvas.height * 0.15,
                heroCanvas.width / 2, heroCanvas.height / 2, heroCanvas.width * 0.6
            );
            bgGrad.addColorStop(0, '#3D0A05');
            bgGrad.addColorStop(0.55, '#200402');
            bgGrad.addColorStop(1, '#110202');
            ctx.fillStyle = bgGrad;
            ctx.fillRect(0, 0, heroCanvas.width, heroCanvas.height);

            // Apply high-DPI transform so coordinates operate in CSS pixels
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
            ctx.imageSmoothingEnabled = true;
            ctx.imageSmoothingQuality = 'high';

            const imageWidth = img.naturalWidth || 1280;
            const imageHeight = img.naturalHeight || 720;

            // Strict contain-style scaling:
            const scale = Math.min(
                availableWidth / imageWidth,
                availableHeight / imageHeight
            );

            const drawWidth = imageWidth * scale;
            const drawHeight = imageHeight * scale;

            const x = (availableWidth - drawWidth) / 2;
            const y = (availableHeight - drawHeight) / 2;

            ctx.drawImage(img, x, y, drawWidth, drawHeight);

            // Synchronize right-side promo cards
            updateRightPanel(index);
        }

        // Preload frames from '1-Hero Section/left side hero animation/' and right-side images
        function preloadFrames() {
            // Preload right-side images for instant display
            const right1 = new Image();
            right1.src = '/images/hero/1.png';
            const right2 = new Image();
            right2.src = '/images/hero/2.png';

            for (let i = 1; i <= FRAME_COUNT; i++) {
                const img = new Image();
                const frameIdx = i - 1;
                img.onload = () => {
                    loadedCount++;
                    if (heroLoaderBar) {
                        heroLoaderBar.style.width = `${(loadedCount / FRAME_COUNT) * 100}%`;
                        if (loadedCount === FRAME_COUNT) {
                            setTimeout(() => { heroLoaderBar.style.opacity = '0'; }, 300);
                        }
                    }

                    // Render first available frame immediately
                    if (!isFirstRendered && frameIdx === 0) {
                        isFirstRendered = true;
                        resizeCanvas();
                        renderFrame(0);
                        updateRightPanel(0);
                    } else if (isFirstRendered && frameIdx === currentFrameIndex) {
                        renderFrame(currentFrameIndex);
                    }
                };
                img.onerror = () => {
                    loadedCount++;
                    if(heroLoaderBar){heroLoaderBar.style.width=`${loadedCount / FRAME_COUNT * 100}%`;if(loadedCount===FRAME_COUNT)heroLoaderBar.style.opacity='0';}
                    if(frameIdx===0)resizeCanvas();
                };
                img.src = getFramePath(i);
                images.push(img);
            }

            // Fallback: render immediately if frame 1 is already in cache
            setTimeout(() => {
                if (!isFirstRendered && images[0] && images[0].complete && images[0].naturalWidth > 0) {
                    isFirstRendered = true;
                    resizeCanvas();
                    renderFrame(0);
                    updateRightPanel(0);
                }
            }, 80);
        }

        let targetFrameIndex = 0;
        let smoothFrameIndex = 0;
        let isLerping = false;

        // Cinematic frame interpolation for a calm, graceful animation
        function animateFrames() {
            const diff = targetFrameIndex - smoothFrameIndex;
            if (Math.abs(diff) > 0.05) {
                smoothFrameIndex += diff * 0.12; // Smooth damping factor
                const renderIdx = Math.min(Math.max(Math.round(smoothFrameIndex), 0), FRAME_COUNT - 1);
                if (renderIdx !== currentFrameIndex) {
                    currentFrameIndex = renderIdx;
                    renderFrame(currentFrameIndex);
                }
                window.requestAnimationFrame(animateFrames);
            } else {
                smoothFrameIndex = targetFrameIndex;
                if (currentFrameIndex !== targetFrameIndex) {
                    currentFrameIndex = targetFrameIndex;
                    renderFrame(currentFrameIndex);
                }
                isLerping = false;
            }
        }

        // Scroll progress calculation
        function updateHeroOnScroll() {
            const rect = heroSection.getBoundingClientRect();
            const navbar = document.getElementById('mainNavbar');
            const navbarHeight = navbar ? navbar.offsetHeight : 76;
            const stickyEl = heroStickyContainer || document.getElementById('heroStickyContainer');
            const stickyHeight = stickyEl ? stickyEl.offsetHeight : (window.innerHeight - navbarHeight);
            const totalScrollable = heroSection.offsetHeight - stickyHeight;
            if (totalScrollable <= 0) return;

            // Scrolled offset from when top of heroSection reaches bottom of navbar
            const scrolled = Math.max(0, navbarHeight - rect.top);
            let progress = scrolled / totalScrollable;
            progress = Math.min(Math.max(progress, 0), 1);

            // Frame mapping for 108 frames (indices 0 to 107):
            // Phase 1 (Frames 1-40): 0% to 30% of scroll distance (gentle, steady spin)
            // Phase 2 (Frames 41-53 Slow Morph): 30% to 65% of scroll distance (extra slow 35% window for burger-to-cake transformation)
            // Phase 3 (Frames 54-108): 65% to 100% of scroll distance (steady cake showcase)
            let calculatedIndex = 0;
            if (progress <= 0.30) {
                const ratio = progress / 0.30;
                calculatedIndex = Math.floor(ratio * 40); // 0 to 39 (Frames 1 to 40)
            } else if (progress <= 0.65) {
                const ratio = (progress - 0.30) / 0.35;
                calculatedIndex = 40 + Math.floor(ratio * 13); // 40 to 52 (Frames 41 to 53 - Extra slow transformation)
            } else {
                const ratio = Math.min((progress - 0.65) / 0.35, 1.0);
                calculatedIndex = 53 + Math.floor(ratio * 55); // 53 to 107 (Frames 54 to 108)
            }

            targetFrameIndex = Math.min(Math.max(calculatedIndex, 0), FRAME_COUNT - 1);

            if (!isLerping) {
                isLerping = true;
                window.requestAnimationFrame(animateFrames);
            }

            // Smoothly fade scroll indicator on initial scroll
            if (heroScrollIndicator) {
                if (progress > 0.08) {
                    heroScrollIndicator.style.opacity = '0';
                } else {
                    heroScrollIndicator.style.opacity = (1 - (progress / 0.08)).toFixed(2);
                }
            }
        }

        window.addEventListener('resize', resizeCanvas);
        window.addEventListener('scroll', () => {
            window.requestAnimationFrame(updateHeroOnScroll);
        }, { passive: true });

        preloadFrames();
        resizeCanvas();
        updateRightPanel(0);
    }

    /* ==========================================================================
       SECTION 2: HORIZONTAL GALLERY OPPOSITE-DIRECTION SCROLL
       ========================================================================== */
    const gallerySection = document.getElementById('gallerySection');
    const row1 = document.getElementById('galleryRow1'); // Moves Right to Left
    const row2 = document.getElementById('galleryRow2'); // Moves Left to Right

    if (gallerySection && row1 && row2) {
        let baseOffset = 0;

        function updateGalleryMotion() {
            const rect = gallerySection.getBoundingClientRect();
            const winH = window.innerHeight;

            // Check if gallery is anywhere near viewport
            if (rect.bottom > -200 && rect.top < winH + 200) {
                // Progress as gallery passes across the viewport
                const progress = (winH - rect.top) / (winH + rect.height);
                
                // Row 1: Right to Left (negative translation)
                const shiftRow1 = - (progress * 280);
                row1.style.transform = `translateX(${shiftRow1}px)`;

                // Row 2: Left to Right (positive translation)
                const shiftRow2 = (progress * 280) - 140;
                row2.style.transform = `translateX(${shiftRow2}px)`;
            }
        }

        window.addEventListener('scroll', () => {
            window.requestAnimationFrame(updateGalleryMotion);
        }, { passive: true });

        updateGalleryMotion();
    }

    /* ==========================================================================
       CART & NOTIFICATION STATE UTILITIES (Connected to H2 cart_items backend)
       ========================================================================== */
    window.getCartGuestId = function () {
        let gid = localStorage.getItem('sweetbloom_cart_guest_id');
        if (!gid) {
            gid = 'guest_' + Math.random().toString(36).substring(2, 11) + Date.now().toString(36);
            localStorage.setItem('sweetbloom_cart_guest_id', gid);
        }
        return gid;
    };

    window.BakeryCart = {
        getItems: function () {
            try {
                return JSON.parse(localStorage.getItem('sweet_bloom_cart')) || [];
            } catch (e) {
                return [];
            }
        },
        saveItems: function (items) {
            localStorage.setItem('sweet_bloom_cart', JSON.stringify(items));
            this.updateBadge();
        },
        syncWithBackend: async function () {
            try {
                const res = await fetch('/api/site/cart', {
                    headers: { 'X-Guest-ID': window.getCartGuestId() }
                });
                if (res.ok) {
                    const data = await res.json();
                    if (data && Array.isArray(data.items)) {
                        const items = data.items.map(it => ({
                            cartItemId: it.id,
                            id: it.productId,
                            name: it.productName,
                            price: it.unitPrice,
                            quantity: it.quantity,
                            selectedSize: it.selectedSize,
                            subtotal: it.subtotal,
                            image: it.imageUrl || ''
                        }));
                        localStorage.setItem('sweet_bloom_cart', JSON.stringify(items));
                        this.updateBadge();
                        return items;
                    }
                }
            } catch (e) {}
            return this.getItems();
        },
        addItem: async function (item) {
            const items = this.getItems();
            const existing = items.find(i => i.id === item.id && (i.selectedSize || 'Small') === (item.selectedSize || 'Small'));
            if (existing) {
                existing.quantity += (item.quantity || 1);
            } else {
                items.push({
                    id: item.id,
                    name: item.name,
                    price: parseFloat(item.price) || 0,
                    quantity: item.quantity || 1,
                    selectedSize: item.selectedSize || 'Small',
                    image: item.image || ''
                });
            }
            this.saveItems(items);
            this.showToast(`Added ${item.name} to your basket!`);

            try {
                await fetch('/api/site/cart/add', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-Guest-ID': window.getCartGuestId()
                    },
                    body: JSON.stringify({
                        productId: item.id,
                        productName: item.name,
                        productType: item.productType || 'BAKERY',
                        selectedSize: item.selectedSize || 'Small',
                        quantity: item.quantity || 1,
                        unitPrice: parseFloat(item.price) || 0,
                        imageUrl: item.image || ''
                    })
                });
                this.syncWithBackend();
            } catch (e) {}
        },
        removeItem: async function (id, cartItemId) {
            let items = this.getItems();
            const target = items.find(i => i.id === id || (cartItemId && i.cartItemId === cartItemId));
            items = items.filter(i => i.id !== id && (!cartItemId || i.cartItemId !== cartItemId));
            this.saveItems(items);

            const cId = cartItemId || (target ? target.cartItemId : null);
            if (cId) {
                try {
                    await fetch(`/api/site/cart/${cId}/delete`, {
                        method: 'POST',
                        headers: { 'X-Guest-ID': window.getCartGuestId() }
                    });
                } catch (e) {}
            }
        },
        updateQuantity: async function (id, qty, cartItemId) {
            const items = this.getItems();
            const it = items.find(i => i.id === id || (cartItemId && i.cartItemId === cartItemId));
            if (it) {
                it.quantity = Math.max(1, qty);
                this.saveItems(items);
                const cId = cartItemId || it.cartItemId;
                if (cId) {
                    try {
                        await fetch(`/api/site/cart/${cId}/update?quantity=${it.quantity}`, {
                            method: 'POST',
                            headers: { 'X-Guest-ID': window.getCartGuestId() }
                        });
                    } catch (e) {}
                }
            }
        },
        clear: async function () {
            localStorage.removeItem('sweet_bloom_cart');
            this.updateBadge();
            try {
                await fetch('/api/site/cart/clear', {
                    method: 'POST',
                    headers: { 'X-Guest-ID': window.getCartGuestId() }
                });
            } catch (e) {}
        },
        getCount: function () {
            const items = this.getItems();
            return items.reduce((sum, i) => sum + i.quantity, 0);
        },
        getTotal: function () {
            const items = this.getItems();
            return items.reduce((sum, i) => sum + (i.price * i.quantity), 0);
        },
        updateBadge: function () {
            const count = this.getCount();
            const badges = document.querySelectorAll('.cart-badge, #navCartBadge, #navCartCount');
            badges.forEach(b => {
                b.textContent = count;
                b.style.display = count > 0 ? 'flex' : 'none';
            });
        },
        showToast: function (msg) {
            let toast = document.getElementById('bakeryToast');
            if (!toast) {
                toast = document.createElement('div');
                toast.id = 'bakeryToast';
                toast.style.cssText = `
                    position: fixed;
                    bottom: 2rem;
                    right: 2rem;
                    background: #2C1E16;
                    color: #FFFDF9;
                    padding: 0.9rem 1.6rem;
                    border-radius: 9999px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.25);
                    z-index: 9999;
                    font-size: 0.95rem;
                    font-weight: 600;
                    border-left: 4px solid #C92C3D;
                    transition: transform 0.3s ease, opacity 0.3s ease;
                    transform: translateY(20px);
                    opacity: 0;
                    display: flex;
                    align-items: center;
                    gap: 0.5rem;
                `;
                document.body.appendChild(toast);
            }
            toast.textContent = '🥐 ' + msg;
            toast.style.transform = 'translateY(0)';
            toast.style.opacity = '1';
            clearTimeout(window._toastTimeout);
            window._toastTimeout = setTimeout(() => {
                toast.style.transform = 'translateY(20px)';
                toast.style.opacity = '0';
            }, 3000);
        }
    };

    /* ==========================================================================
       AUTHENTICATION & NAVBAR PROFILE ICON MANAGEMENT
       ========================================================================== */
    window.SweetBloomAuth = {
        currentUser: null,

        async checkSession() {
            try {
                let res = await fetch('/api/site/auth/session');
                if (!res.ok) res = await fetch('/api/site/auth/me');
                if (res.ok) {
                    const data = await res.json();
                    if (data && data.authenticated) {
                        const userObj = data.user || data;
                        this.currentUser = userObj;
                        return userObj;
                    }
                }
            } catch (e) {}

            localStorage.removeItem('sweetbloom_user');
            this.currentUser = null;
            return null;
        },

        async logout() {
            try {
                await fetch('/api/site/auth/logout', { method: 'POST' });
            } catch (e) {}
            localStorage.removeItem('sweetbloom_user');
            window.location.href = '/login.html?logout=true';
        },

        async initNavbarProfile() {
            const wrapper = document.getElementById('navProfileWrapper');
            const navLinks = document.querySelector('.nav-links');
            const path = window.location.pathname;

            const user = await this.checkSession();
            if (user && user.role === 'ADMIN') {
                if (navLinks) {
                    navLinks.innerHTML = `
                        <li><a href="admin.html" class="nav-link ${path === 'admin.html' || (path.includes('admin') && !path.includes('/orders') && !path.includes('/bookings')) ? 'active' : ''}">Dashboard</a></li>
                        <li><a href="admin.html?tab=orders" class="nav-link ${window.location.search.includes('tab=orders') ? 'active' : ''}">Order Management</a></li>
                        <li><a href="admin.html?tab=bookings" class="nav-link ${window.location.search.includes('tab=bookings') ? 'active' : ''}">Booking Management</a></li>
                        <li><a href="admin.html#products" class="nav-link">Product Management</a></li>
                        <li><a href="admin.html#users" class="nav-link">User Management</a></li>
                    `;
                }
                if (wrapper) {
                    wrapper.innerHTML = `
                        <button type="button" class="btn-profile-nav is-logged-in" id="navProfileBtn" aria-expanded="false" title="Admin Account">
                            <span class="profile-nav-icon">⚙️</span>
                            <span class="profile-nav-name">Admin</span>
                        </button>
                        <div class="profile-dropdown-menu" id="profileDropdownMenu">
                            <div class="profile-dropdown-header">
                                <div class="profile-dropdown-name">${user.fullName || 'Admin User'}</div>
                                <div class="profile-dropdown-email">${user.email || ''}</div>
                            </div>
                            <a href="admin.html" class="profile-dropdown-item">📊 Admin Dashboard</a>
                            <a href="admin.html?tab=orders" class="profile-dropdown-item">📋 Order Management</a>
                            <a href="admin.html?tab=bookings" class="profile-dropdown-item">🎂 Booking Management</a>
                            <div class="profile-dropdown-divider"></div>
                            <a href="javascript:void(0)" class="profile-dropdown-item logout-item" onclick="window.SweetBloomAuth.logout()">🚪 Logout</a>
                        </div>
                    `;
                }
            } else if (user && (user.role === 'CUSTOMER' || user.userId)) {
                if (navLinks) {
                    const isHome = path === '/' || path.endsWith('/index.html') || path.endsWith('/index.html') || path.endsWith('/index');
                    const isBakery = path.includes('bakery');
                    const isCake = path.includes('custom-cake') || path.includes('cake-booking');
                    const isOrders = path.includes('my-orders') || path.includes('orders');
                    const isBookings = path.includes('my-bookings') || path.includes('bookings');
                    const isProfile = path.includes('account') || path.includes('profile');

                    navLinks.innerHTML = `
                        <li><a href="/index.html" class="nav-link ${isHome ? 'active' : ''}">Home</a></li>
                        <li><a href="/bakery.html" class="nav-link ${isBakery ? 'active' : ''}">Daily Bakery</a></li>
                        <li><a href="custom-cake.html" class="nav-link ${isCake ? 'active' : ''}">Book Cake</a></li>
                        <li><a href="orders.html?tab=orders" class="nav-link ${isOrders ? 'active' : ''}">My Orders</a></li>
                        <li><a href="orders.html?tab=bookings" class="nav-link ${isBookings ? 'active' : ''}">My Bookings</a></li>
                        <li><a href="account.html" class="nav-link ${isProfile ? 'active' : ''}">Profile</a></li>
                    `;
                }
                if (wrapper) {
                    const firstName = (user.fullName || 'Customer').trim().split(' ')[0];
                    wrapper.innerHTML = `
                        <button type="button" class="btn-profile-nav is-logged-in" id="navProfileBtn" aria-expanded="false" title="My Account">
                            <span class="profile-nav-icon">👤</span>
                            <span class="profile-nav-name" id="navProfileName">${firstName}</span>
                        </button>
                        <div class="profile-dropdown-menu" id="profileDropdownMenu">
                            <div class="profile-dropdown-header">
                                <div class="profile-dropdown-name">${user.fullName || 'Customer'}</div>
                                <div class="profile-dropdown-email">${user.email || ''}</div>
                            </div>
                            <a href="account.html" class="profile-dropdown-item">👤 Profile Details</a>
                            <a href="orders.html?tab=orders" class="profile-dropdown-item">📦 My Orders</a>
                            <a href="orders.html?tab=bookings" class="profile-dropdown-item">🎂 My Bookings</a>
                            <a href="account.html#deliveries" class="profile-dropdown-item">🚚 Delivery Information</a>
                            <div class="profile-dropdown-divider"></div>
                            <a href="javascript:void(0)" class="profile-dropdown-item logout-item" onclick="window.SweetBloomAuth.logout()">🚪 Logout</a>
                        </div>
                    `;
                }
            } else {
                if (navLinks) {
                    const isHome = path === '/' || path.endsWith('/index.html') || path.endsWith('/index.html') || path.endsWith('/index');
                    const isBakery = path.includes('bakery');
                    const isCake = path.includes('custom-cake') || path.includes('cake-booking');

                    navLinks.innerHTML = `
                        <li><a href="/index.html" class="nav-link ${isHome ? 'active' : ''}">Home</a></li>
                        <li><a href="/bakery.html" class="nav-link ${isBakery ? 'active' : ''}">Daily Bakery</a></li>
                        <li><a href="custom-cake.html" class="nav-link ${isCake ? 'active' : ''}">Book Cake</a></li>
                    `;
                }
                if (wrapper) {
                    wrapper.innerHTML = `
                        <a href="/login" class="btn-profile-nav" id="navProfileBtn" title="Customer Login / Sign Up">
                            <span class="profile-nav-icon">👤</span>
                            <span class="profile-nav-name">Sign In</span>
                        </a>
                    `;
                }
            }

            if (wrapper) {
                const btn = wrapper.querySelector('#navProfileBtn');
                const menu = wrapper.querySelector('#profileDropdownMenu');
                if (btn && menu) {
                    btn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        menu.classList.toggle('show');
                        btn.setAttribute('aria-expanded', menu.classList.contains('show'));
                    });
                    document.addEventListener('click', (e) => {
                        if (!wrapper.contains(e.target)) {
                            menu.classList.remove('show');
                            btn.setAttribute('aria-expanded', 'false');
                        }
                    });
                }
            }
        }
    };

    async function initNavbarCategories() {
        const bakeryLink = document.getElementById('navBakery') || document.querySelector('a[href*="bakery.html"]');
        const customCakeLink = document.getElementById('navCustomCake') || document.querySelector('a[href*="custom-cake.html"]');

        if (!bakeryLink && !customCakeLink) return;

        try {
            const res = await fetch('/api/site/categories');
            if (!res.ok) return;
            const categories = await res.json();
            if (!Array.isArray(categories)) return;

            // Daily Bakery categories
            if (bakeryLink) {
                const bakeryParent = bakeryLink.parentElement;
                if (bakeryParent && bakeryParent.tagName === 'LI') {
                    const bakeryCats = categories.filter(c => c.section === 'DAILY_BAKERY');
                    if (bakeryCats.length > 0) {
                        bakeryParent.classList.add('has-dropdown');
                        let dropdown = bakeryParent.querySelector('.nav-category-dropdown');
                        if (!dropdown) {
                            dropdown = document.createElement('div');
                            dropdown.className = 'nav-category-dropdown';
                            bakeryParent.appendChild(dropdown);
                        }
                        dropdown.innerHTML = bakeryCats.map(cat => `
                            <a href="bakery.html?category=${encodeURIComponent(cat.name)}" class="nav-category-item">
                                <span class="cat-icon">🥖</span>
                                <span>${cat.name}</span>
                            </a>
                        `).join('');
                    }
                }
            }

            // Custom Cake categories
            if (customCakeLink) {
                const cakeParent = customCakeLink.parentElement;
                if (cakeParent && cakeParent.tagName === 'LI') {
                    const cakeCats = categories.filter(c => c.section === 'CUSTOM_CAKE');
                    if (cakeCats.length > 0) {
                        cakeParent.classList.add('has-dropdown');
                        let dropdown = cakeParent.querySelector('.nav-category-dropdown');
                        if (!dropdown) {
                            dropdown = document.createElement('div');
                            dropdown.className = 'nav-category-dropdown';
                            cakeParent.appendChild(dropdown);
                        }
                        dropdown.innerHTML = cakeCats.map(cat => `
                            <a href="custom-cake.html?category=${encodeURIComponent(cat.name)}" class="nav-category-item">
                                <span class="cat-icon">🎂</span>
                                <span>${cat.name}</span>
                            </a>
                        `).join('');
                    }
                }
            }
        } catch (e) {
            console.warn('Navbar category dynamic load note:', e);
        }
    }

    // Initialize badge, section scroll observers, and navbar profile on load
    document.addEventListener('DOMContentLoaded', () => {
        window.BakeryCart.updateBadge();
        window.BakeryCart.syncWithBackend();
        if (window.SweetBloomAuth) {
            window.SweetBloomAuth.initNavbarProfile();
        }
        initNavbarCategories();

        // How It Works Stagger Reveal
        const flowContainer = document.querySelector('.hiw-flow-container');
        if (flowContainer) {
            const revealHIW = () => flowContainer.classList.add('is-visible');
            if ('IntersectionObserver' in window) {
                const hiwObserver = new IntersectionObserver((entries) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            revealHIW();
                            hiwObserver.unobserve(entry.target);
                        }
                    });
                }, { threshold: 0.05 });
                hiwObserver.observe(flowContainer);
            } else {
                revealHIW();
            }
            // Fallback safety timeout so section is guaranteed to show
            setTimeout(revealHIW, 600);
        }

        // Why Choose Us Stagger Reveal
        const wcuContainer = document.querySelector('.wcu-cards-grid');
        if (wcuContainer) {
            const revealWCU = () => wcuContainer.classList.add('is-visible');
            if ('IntersectionObserver' in window) {
                const wcuObserver = new IntersectionObserver((entries) => {
                    entries.forEach(entry => {
                        if (entry.isIntersecting) {
                            revealWCU();
                            wcuObserver.unobserve(entry.target);
                        }
                    });
                }, { threshold: 0.05 });
                wcuObserver.observe(wcuContainer);
            } else {
                revealWCU();
            }
            // Fallback safety timeout so section is guaranteed to show
            setTimeout(revealWCU, 800);
        }
    });

})();
