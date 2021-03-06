{-# LANGUAGE TypeOperators #-}

module HW0.T1 where

data a <-> b = Iso (a -> b) (b -> a)

flipIso :: (a <-> b) -> (b <-> a)
flipIso (Iso f g) = Iso g f

runIso :: (a <-> b) -> (a -> b)
runIso (Iso f _) = f

distrib :: Either a (b, c) -> (Either a b, Either a c)
distrib (Left a) = (Left a, Left a)
distrib (Right (b, c)) = (Right b, Right c)

assocPair :: (a, (b, c)) <-> ((a, b), c)
assocPair = Iso forward backwards where
  forward (a, (b, c)) = ((a, b), c)
  backwards ((a, b), c) = (a, (b, c))

assocEither :: Either a (Either b c) <-> Either (Either a b) c
assocEither = (Iso forward backwards) where
  forward (Left a) = Left (Left a)
  forward (Right (Left b)) = Left (Right b)
  forward (Right (Right c)) = Right c
  backwards (Left (Left a)) = Left a
  backwards (Left (Right b)) = Right (Left b)
  backwards (Right c) = Right (Right c)