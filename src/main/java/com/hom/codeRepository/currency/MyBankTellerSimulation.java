package com.hom.codeRepository.currency;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;

/**
 * Java编程思想 21.8 
 * 模拟生产者消费者
 * @author Hom
 *
 */
class Customer
{
	final int serviceTime;
	public Customer(int serviceTime) {
		this.serviceTime = serviceTime;
	}
}

class CustomerLine extends ArrayBlockingQueue<Customer>
{
	private static final long serialVersionUID = -685088409345400419L;

	public CustomerLine(int capacity) {
		super(capacity);
	}
	
	@Override
	public String toString() {
		String info = "";
		
		Iterator<Customer> it = this.iterator();
		while(it.hasNext())
		{
			Customer c = it.next();
			info += "["+c.serviceTime+"]";
		}
		
		return info;
	}
}

class Teller implements Runnable{
	
	private int id = 0;
	private int customersServed = 0; 
	private CustomerLine  customerLine;
	private boolean working = true;
	public Teller(CustomerLine customerLine, int id) {
		this.customerLine = customerLine;
		this.working = true;
		this.id = id;
	}
	
	public void doService()
	{
		working = true;
	}

	public void doOtherThing(){
		customersServed = 0;
		working = false;
	}
	/*public void doOtherThing()
	{
		long timeout = RandomUtils.nextLong(0, 1000);
		try {
			TimeUnit.MILLISECONDS.sleep(timeout);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}*/
		
	@Override
	public void run() {
		try {
			while(!working){
//				wait();
				TimeUnit.MILLISECONDS.sleep(50);
			}
			Customer c = customerLine.take();
			TimeUnit.MILLISECONDS.sleep(c.serviceTime);
			customersServed++;
//			System.out.println(customerLine);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return "{"+id+",一共服务了:"+customersServed+"}";
	}
}

class CustomerProductor implements Runnable
{
	private CustomerLine customerLine;
	public CustomerProductor(CustomerLine customerLine) {
		this.customerLine = customerLine;
	}
	
	@Override
	public void run() {
		while(true)
		{
			int randNum = RandomUtils.nextInt(0, 5);
			for(int i=0; i<randNum; i++)
			{
				int randServiceTime = RandomUtils.nextInt(0, 50);
				Customer c = new Customer(randServiceTime);
				this.customerLine.add(c);
				System.out.println(customerLine);
			}
			
			long timeout = RandomUtils.nextLong(0, 1000);
			try {
				TimeUnit.MILLISECONDS.sleep(timeout);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class TellerManager implements Runnable {

	private int tellerId;
	private ExecutorService exec;
	private CustomerLine customerLine;
	
	public TellerManager(ExecutorService exec,
			CustomerLine customerLine) {
		this.customerLine = customerLine;
		this.exec = exec;
	}
	
	private ArrayBlockingQueue<Teller> workingTellers = 
			new ArrayBlockingQueue<>(100);
	
	private ArrayBlockingQueue<Teller> restTellers =
			new ArrayBlockingQueue<>(100);
	
	@Override
	public void run() {
		Teller first = new Teller(customerLine, tellerId++);
		exec.submit(first);
		
		while(true)
		{
			adjustList();
		}
	}
	
	private void adjustList()
	{
		if(customerLine.size() > workingTellers.size()*2)
		{
			if(restTellers.size() > 0)
			{
				Teller rester = restTellers.poll();
				if(rester!=null){
					boolean flag = workingTellers.offer(rester); 
					if(!flag){
						//暂时不考虑添加不成功的情况
					}
				}
			}
			else 
			{
				Teller teller = new Teller(customerLine, tellerId++);
				exec.submit(teller);
				workingTellers.add(teller);
				System.out.println("雇佣一个出纳员，人数："+workingTellers.size());
			}
		}
		else if(customerLine.size() < workingTellers.size()/2)
		{
			Teller worker = workingTellers.poll();
			if(worker != null)
			{
				restTellers.offer(worker);
			}
			System.out.println("休息了:"+worker.toString());
			worker.doOtherThing();
		}
	}
}

class Manager implements Runnable
{
	private CustomerLine customerLine; 
	
	public Manager(CustomerLine customerLine) {
		this.customerLine = customerLine;
	}
	
	@Override
	public void run() {
		while(true){
			System.out.println(customerLine);
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}


public class MyBankTellerSimulation {
	
	public static void main(String[] args) {
		CustomerLine customerLine = new CustomerLine(100);
		ExecutorService exec = Executors.newCachedThreadPool();
				
		CustomerProductor productor = new CustomerProductor(customerLine);
//		Teller teller = new Teller(customerLine);
		TellerManager manager = new TellerManager(exec, customerLine);

		exec.submit(productor);
//		exec.submit(teller);
		exec.submit(manager);

		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		exec.shutdown();
	}
			
}
