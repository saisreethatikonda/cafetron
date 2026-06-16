package com.cafetron.wallet.service;

import com.cafetron.wallet.dto.PagedTransactionDto;
import com.cafetron.wallet.dto.TransactionResponseDto;
import com.cafetron.wallet.dto.WalletResponseDto;
import com.cafetron.wallet.entity.Transaction;
import com.cafetron.wallet.entity.TransactionType;
import com.cafetron.wallet.entity.Wallet;
import com.cafetron.wallet.exception.InsufficientFundsException;
import com.cafetron.wallet.exception.WalletNotFoundException;
import com.cafetron.wallet.repository.TransactionRepository;
import com.cafetron.wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount, String description) {
        validateCommonInputs(userId, amount, description, "debit");

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(wallet.getBalance(), amount);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setDescription(description.trim());
        transaction.setType(TransactionType.DEBIT);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void refund(Long userId, BigDecimal amount, String description) {
        validateCommonInputs(userId, amount, description, "refund");

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setDescription(description.trim());
        transaction.setType(TransactionType.REFUND);
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void topUp(Long userId, BigDecimal amount) {
        validateUserAndAmount(userId, amount, "top-up");

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TOP_UP);
        transaction.setDescription("Wallet top-up");
        transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        return WalletResponseDto.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUser().getId())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedTransactionDto getTransactions(Long userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new WalletNotFoundException(userId));

        Page<Transaction> page = transactionRepository.findByWallet_IdOrderByCreatedAtDesc(wallet.getId(), pageable);
        List<TransactionResponseDto> transactions = page.getContent().stream()
                .map(this::mapToTransactionDto)
                .toList();

        return PagedTransactionDto.builder()
                .transactions(transactions)
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }

    private TransactionResponseDto mapToTransactionDto(Transaction transaction) {
        return TransactionResponseDto.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .orderId(transaction.getOrder() != null ? transaction.getOrder().getId() : null)
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private void validateCommonInputs(Long userId, BigDecimal amount, String description, String operation) {
        validateUserAndAmount(userId, amount, operation);
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required for wallet " + operation);
        }
    }

    private void validateUserAndAmount(Long userId, BigDecimal amount, String operation) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required for wallet " + operation);
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required for wallet " + operation);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero for wallet " + operation);
        }
    }
}
