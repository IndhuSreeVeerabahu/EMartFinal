# E-Commerce Application Deployment Guide

## Railway Deployment Configuration

This guide will help you deploy your E-Commerce application to Railway with the new PostgreSQL database configuration.

### Repository Information
- **Repository**: https://github.com/IndhuSreeVeerabahu/EMartFinal.git
- **Database**: PostgreSQL on Railway

### Database Configuration

#### Database URLs
- **Public URL**: `postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@shortline.proxy.rlwy.net:35449/railway`
- **Internal URL**: `postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@postgres.railway.internal:5432/railway`

### Environment Variables for Railway

Set these environment variables in your Railway project dashboard:

#### Core Application Configuration
```
SPRING_PROFILES_ACTIVE=prod
PORT=8080
APP_ENVIRONMENT=production
```

#### Database Configuration
```
DATABASE_URL=postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@shortline.proxy.rlwy.net:35449/railway
DB_URL=postgresql://postgres:dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR@postgres.railway.internal:5432/railway
DB_USERNAME=postgres
DB_PASSWORD=dzVSSNhjjQDshMpaVvMZapwCXnqlQrJR
```

#### Admin Account
```
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123
```

#### Cashfree Payment Gateway (Sandbox Mode)
```
CASHFREE_APP_ID=TEST108283821957fe1153788f32479528382801
CASHFREE_SECRET_KEY=cfsk_ma_test_dddc2fa7d13c09a0a5f0c9c88f26f678_f1e97d01
CASHFREE_ENVIRONMENT=SANDBOX
CASHFREE_RETURN_URL=https://emartfinal-production.up.railway.app/payment/success
CASHFREE_NOTIFY_URL=https://emartfinal-production.up.railway.app/payment/webhook
```

### Deployment Steps

1. **Connect Repository to Railway**
   - Go to Railway dashboard
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your repository: `IndhuSreeVeerabahu/EMartFinal`

2. **Set Environment Variables**
   - In your Railway project, go to "Variables" tab
   - Add all the environment variables listed above
   - Make sure to use the exact values provided

3. **Configure Database**
   - Add a PostgreSQL service to your Railway project
   - The database URLs are already configured in the environment variables
   - Railway will automatically connect your app to the database

4. **Deploy**
   - Railway will automatically build and deploy your application
   - The deployment uses the Dockerfile in your repository
   - Monitor the deployment logs for any issues

### Application Features

Your E-Commerce application includes:
- **User Management**: Registration, login, profile management
- **Product Catalog**: Browse products, categories, search
- **Shopping Cart**: Add/remove items, quantity management
- **Payment Integration**: Cashfree payment gateway (sandbox mode)
- **Admin Panel**: Product management, order management
- **Order Management**: Order tracking, status updates
- **Email Notifications**: Order confirmations, status updates

### Access Points

After successful deployment:
- **Main Application**: `https://emartfinal-production.up.railway.app`
- **Admin Panel**: `https://emartfinal-production.up.railway.app/admin`
- **API Documentation**: `https://emartfinal-production.up.railway.app/swagger-ui.html`
- **Health Check**: `https://emartfinal-production.up.railway.app/actuator/health`

### Default Admin Credentials
- **Username**: `admin`
- **Password**: `admin123`

### Payment Gateway Configuration

The application is configured with Cashfree payment gateway in sandbox mode:
- **Test Mode**: All transactions are simulated
- **Return URL**: Handles successful payments
- **Webhook URL**: Receives payment status updates

### Monitoring and Logs

- **Application Logs**: Available in Railway dashboard
- **Health Checks**: Configured at `/actuator/health`
- **Metrics**: Basic application metrics available

### Troubleshooting

1. **Database Connection Issues**
   - Verify environment variables are set correctly
   - Check database service is running in Railway
   - Review application logs for connection errors

2. **Payment Gateway Issues**
   - Ensure webhook URLs are accessible
   - Verify Cashfree credentials are correct
   - Check sandbox mode configuration

3. **Application Startup Issues**
   - Review deployment logs
   - Check Java memory settings
   - Verify all required environment variables are set

### Security Notes

- Change default admin password in production
- Use production Cashfree keys for live transactions
- Enable HTTPS in production
- Review and update security configurations

### Support

For deployment issues:
1. Check Railway deployment logs
2. Review application logs
3. Verify environment variable configuration
4. Test database connectivity

---

**Last Updated**: January 2025
**Repository**: https://github.com/IndhuSreeVeerabahu/EMartFinal.git
