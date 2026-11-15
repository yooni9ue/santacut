import { useState, type FormEvent } from 'react';
import { registerParticipant, type Participant, type Region } from './api';

interface RegistrationFormProps {
  regions: Region[];
  onRegistered: (participant: Participant) => void;
}

export function RegistrationForm({ regions, onRegistered }: RegistrationFormProps) {
  const [nickname, setNickname] = useState('');
  const [regionCode, setRegionCode] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const participant = await registerParticipant({ nickname: nickname.trim(), regionCode });
      onRegistered(participant);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : '등록 중 오류가 발생했습니다.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="registration-form" onSubmit={submit}>
      <label>
        닉네임
        <input
          autoComplete="nickname"
          maxLength={16}
          minLength={2}
          onChange={(event) => setNickname(event.target.value)}
          placeholder="예: 굴뚝청소부"
          required
          value={nickname}
        />
      </label>
      <label>
        배송 지역
        <select onChange={(event) => setRegionCode(event.target.value)} required value={regionCode}>
          <option disabled value="">시·구를 선택하세요</option>
          {regions.map((region) => (
            <option key={region.code} value={region.code}>{region.name}</option>
          ))}
        </select>
      </label>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="button button-primary" disabled={busy || regions.length === 0} type="submit">
        {busy ? '등록 중…' : '내 산타 프로필 만들기'}
      </button>
      <p className="form-hint">닉네임은 한 번 등록하면 다른 참가자가 사용할 수 없어요.</p>
    </form>
  );
}
