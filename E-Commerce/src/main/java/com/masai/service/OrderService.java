package com.masai.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.masai.beans.Card;
import com.masai.beans.Customer;
import com.masai.beans.Item;
import com.masai.beans.Ordered;
import com.masai.beans.OrderDTO;
import com.masai.beans.Product;
import com.masai.beans.ProductCategory;
import com.masai.exception.NoProductFoundInCart;
import com.masai.repository.OrderCrudRepo;

@Service
public class OrderService implements OrderServiceInterface{

	@Autowired
	private CustomerService customerServ;
	
	@Autowired
	private CartService cartService;
	
	@Autowired 
	private OrderCrudRepo orderCrudRepo;
	
	@Override
	public OrderDTO getOrderStatus(Integer customerId) {
		

		//getting customer using customerID
		Customer customer = customerServ.getCustomerById(customerId);
		
		OrderDTO order = new OrderDTO();
		
		//getting the cart list of that customer
		List<Item> cartList = customer.getCart().getItems();
		
		if(cartList.isEmpty())
			throw new NoProductFoundInCart("No Products are found in the cart to order");
		
		order.setOrderItems(cartList);         
	
		for(Item element : cartList) {
			
			ProductCategory category = element.getProduct().getCategory();
			
			Product product = element.getProduct();
				
			//calculating gst for each categories
			if(category == ProductCategory.ELECTRONICS) {
				
				Double itemGST = element.getProduct().getPrice() * 0.18;
	
				order.setGst(order.getGst() + itemGST);
			}else if(category == ProductCategory.CLOTHES) {
				
				Double itemGST = element.getProduct().getPrice() * 0.12;
				
				order.setGst(order.getGst() + itemGST);
				
			}else if(category == ProductCategory.BOOKS) {
				
				Double itemGST = element.getProduct().getPrice() * 0.05;
				
				order.setGst(order.getGst() + itemGST);
			}		
		}
		
		//cost of all items excluding gst, delivery
		double total = customer.getCart().getCartTotal();
		order.setCost(total);
		
		//setting delivery cost
		order.setDeliveryCost(40);
	
		//final cost paid by customer
		order.setTotalCost(total + order.getGst() + order.getDeliveryCost());
		
		return order;
		
	}
	
	//This method creates the order as the name suggests
	//It is never exposed publically. 
	//Called internally by the placeOrder method.
	//Part of flow as shown below
	//(ENDPOINT) PlaceOrder() --> getOrderStatus() --> cartService.sendToOrder() --> 
	@Override
	public Ordered createOrder(int customerId, String lastFourDigitsOfCardUsed) {
		
		//Calling the order status method to get the details of the order cost
		OrderDTO orderDetails = this.getOrderStatus(customerId);
		
		//Emptying the cart and getting the list that is to be added to the order
		List<Item> itemsOrdered = cartService.sendToOrder(customerId);
		System.out.println(itemsOrdered);
		
		//Creating the order using the cart of the user and the orderDetails
		Ordered order = new Ordered();
		
		//Creation of order
		order.setCardUsedForPayment("XXXXXXXX".concat(lastFourDigitsOfCardUsed));
		order.setDeliveryCharge(orderDetails.getDeliveryCost());
		order.setGst(orderDetails.getGst());
		order.setItemsCost(orderDetails.getCost());
		order.setOrderDate(LocalDate.now());
		order.setTotalAmount(orderDetails.getTotalCost());
//		order.setOrderedItems(itemsOrdered);
		
		//Send the order to the customer to be added to the order list
		customerServ.addCustomerOrder(customerId, order);
		
		return order;
	}
	
	
	//This method persists the order (Called internally only, part of order placement flow)
	@Override 
	public Ordered addOrder(Ordered order) {
		Ordered placedOrder = orderCrudRepo.save(order);
		return placedOrder;
	}
	
	

}
